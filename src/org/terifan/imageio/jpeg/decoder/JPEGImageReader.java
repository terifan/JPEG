package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.JPEGImage;
import org.terifan.imageio.jpeg.SOSSegment;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DQTSegment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP14Segment;
import org.terifan.imageio.jpeg.ColorSpace.ColorSpaceType;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.QuantizationTable;
import static org.terifan.imageio.jpeg.test.Debug.printTables;


public class JPEGImageReader
{
	final static int MAX_CHANNELS = 3;

	private BitInputStream mBitStream;
	private DQTSegment[] mQuantizationTables;
	private int mRestartInterval;
	private SOFSegment mSOFSegment;
	private SOSSegment mSOSSegment;
	private Class<? extends IDCT> mIDCT;
	private ColorSpaceType mColorSpace;
	private boolean mStop;
	private JPEGImage mImage;
	private int[][][][][][] mDctCoefficients;
	private Decoder mDecoder;
	private int mProgressiveLevel;
	private JPEG mJPEG = new JPEG();


	private JPEGImageReader(InputStream aInputStream, Class<? extends IDCT> aIDCT) throws IOException
	{
		mQuantizationTables = new DQTSegment[MAX_CHANNELS];

		mBitStream = new BitInputStream(aInputStream);
		mIDCT = aIDCT;
	}


	public static BufferedImage read(InputStream aInputStream) throws IOException
	{
		return new JPEGImageReader(aInputStream, IDCTIntegerFast.class).readImpl();
	}


	public static BufferedImage read(InputStream aInputStream, Class<? extends IDCT> aDecoder) throws IOException
	{
		return new JPEGImageReader(aInputStream, aDecoder).readImpl();
	}


	private BufferedImage readImpl() throws IOException
	{
		try
		{
			int nextSegment = mBitStream.readInt16();

			if (nextSegment != SOI) // Start Of Image
			{
				throw new IOException("Error in JPEG stream; expected SOI segment marker but found: " + Integer.toString(nextSegment, 16));
			}

			while (!mStop)
			{
				if (mJPEG != null && mJPEG.unread_marker != 0)
				{
					nextSegment = 0xff00 + mJPEG.unread_marker;
					mJPEG.unread_marker = 0;
				}
				else
				{
					nextSegment = mBitStream.readInt16();

					while ((nextSegment & 0xFF00) == 0)
					{
						nextSegment = ((0xFF & nextSegment) << 8) | mBitStream.readInt8();
					}

					if ((nextSegment >> 8) != 255)
					{
						System.out.println("Bad input");

						hexdump();

						break;
	//					throw new IOException("Error in JPEG stream; expected segment marker but found: " + Integer.toString(nextSegment, 16));
					}
				}

//				if (VERBOSE)
				{
					System.out.println(Integer.toString(nextSegment, 16) + " " + getSOFDescription(nextSegment));
				}

				if (nextSegment == EOI)
				{
					break;
				}

				switch (nextSegment)
				{
					case APP0:
						new APP0Segment(mJPEG).read(mBitStream);
						break;
					case APP14:
						new APP14Segment(mJPEG).read(mBitStream);
						break;
					case DQT:
						new DQTSegment(mJPEG).read(mBitStream);
						break;
					case DHT:
						((HuffmanDecoder)mDecoder).readHuffmanTables();
						break;
					case DAC: // Arithmetic Table
						new DACSegment(mJPEG).read(mBitStream);
						break;
					case SOF0: // Baseline
						mDecoder = new HuffmanDecoder(mBitStream);
						mBitStream.setHandleEscapeChars(true);
						mSOFSegment = new SOFSegment(mBitStream, false, false);
						break;
					case SOF1: // Extended sequential, Huffman
						throw new IOException("Image encoding not supported.");
					case SOF2: // Progressive, Huffman
						throw new IOException("Image encoding not supported.");
					case SOF9: // Extended sequential, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
						mBitStream.setHandleEscapeChars(false);
						mSOFSegment = new SOFSegment(mBitStream, true, false);
						break;
					case SOF10: // Progressive, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
						mBitStream.setHandleEscapeChars(false);
						mSOFSegment = new SOFSegment(mBitStream, true, true);
						break;
					case SOF3: // Lossless, Huffman
					case SOF5: // Differential sequential, Huffman
					case SOF6: // Differential progressive, Huffman
					case SOF7: // Differential lossless, Huffman
					case SOF11: // Lossless, arithmetic
					case SOF13: // Differential sequential, arithmetic
					case SOF14: // Differential progressive, arithmetic
					case SOF15: // Differential lossless, arithmetic
						throw new IOException("Image encoding not supported.");
					case SOS:
					{
						System.out.println("======== " + mBitStream.getStreamOffset() + " / " + mProgressiveLevel + " ========================================================================================================================================================================");
						mSOSSegment = new SOSSegment(mBitStream);
						readRaster();
//						if (image.isDamaged() || !true)
//						{
//							return image.getImage();
//						}

						break;
					}
					case DRI:
						mBitStream.skipBytes(2); // skip length
						mRestartInterval = mBitStream.readInt16();
						break;
					case COM:
						mBitStream.skipBytes(mBitStream.readInt16() - 2);
						break;
					default:
						System.out.printf("Unsupported segment: %02X%n", nextSegment);

						mBitStream.skipBytes(mBitStream.readInt16() - 2);
						break;
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		finally
		{
			mBitStream.close();
			mBitStream = null;
		}

		return mImage == null ? null : mImage.getImage();
	}


	private void readRaster() throws IOException
	{
		boolean verbose = false;

		IDCT idct;
		try
		{
			idct = mIDCT.newInstance();
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}

		int restartMarkerIndex = 0;

		try
		{
			mJPEG.Ss = mSOSSegment.getSs();
			mJPEG.Se = mSOSSegment.getSe();
			mJPEG.Ah = mSOSSegment.getAh();
			mJPEG.Al = mSOSSegment.getAl();
			mJPEG.comps_in_scan = mSOSSegment.getNumComponents();
			mJPEG.num_components = mSOFSegment.getNumComponents();
			mJPEG.progressive_mode = mSOFSegment.isProgressive();
			mJPEG.lim_Se = DCTSIZE2-1;

			mJPEG.blocks_in_MCU = 0;

			for (int scanComponentIndex = 0, j = 0; scanComponentIndex < mSOSSegment.getNumComponents(); scanComponentIndex++)
			{
				for (int frameComponentIndex = 0; frameComponentIndex < mSOFSegment.getNumComponents(); frameComponentIndex++)
				{
					if (mSOFSegment.getComponent(frameComponentIndex).getComponentIndex() == mSOSSegment.getComponent(scanComponentIndex))
					{
						ComponentInfo comp = mSOFSegment.getComponent(frameComponentIndex);

						mJPEG.blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
					}
				}
			}
						
			int maxSamplingX = 0;
			int maxSamplingY = 0;

			for (int i = 0; i < mSOFSegment.getNumComponents(); i++)
			{
				ComponentInfo comp = mSOFSegment.getComponent(i);
				maxSamplingX = Math.max(maxSamplingX, comp.getHorSampleFactor());
				maxSamplingY = Math.max(maxSamplingY, comp.getVerSampleFactor());
			}

			int numHorMCU = (mSOFSegment.getWidth() + 8 * maxSamplingX - 1) / (8 * maxSamplingX);
			int numVerMCU = (mSOFSegment.getHeight() + 8 * maxSamplingY - 1) / (8 * maxSamplingY);

			mJPEG.MCU_membership = new int[mJPEG.blocks_in_MCU];
			mJPEG.cur_comp_info = new ComponentInfo[mJPEG.num_components];

			for (int scanComponentIndex = 0, j = 0; scanComponentIndex < mSOSSegment.getNumComponents(); scanComponentIndex++)
			{
				for (int frameComponentIndex = 0; frameComponentIndex < mSOFSegment.getNumComponents(); frameComponentIndex++)
				{
					if (mSOFSegment.getComponent(frameComponentIndex).getComponentIndex() == mSOSSegment.getComponent(scanComponentIndex))
					{
						ComponentInfo comp = mSOFSegment.getComponent(frameComponentIndex);
						comp.setTableAC(mSOSSegment.getACTable(scanComponentIndex));
						comp.setTableDC(mSOSSegment.getDCTable(scanComponentIndex));

						mJPEG.cur_comp_info[scanComponentIndex] = comp;

						for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, j++)
						{
							mJPEG.MCU_membership[j] = scanComponentIndex;
						}

						System.out.println("  SOF: "+comp);
					}
				}
			}

			if (mImage == null)
			{
				mDctCoefficients = new int[numVerMCU][numHorMCU][maxSamplingY][maxSamplingX][mJPEG.num_components][64];

				mDecoder.jinit_decoder(mJPEG);

				mImage = new JPEGImage(mSOFSegment.getWidth(), mSOFSegment.getHeight(), maxSamplingX, maxSamplingY, mJPEG.mDensitiesUnits, mJPEG.mDensityX, mJPEG.mDensityY, mJPEG.num_components);
			}

			mDecoder.start_pass(mJPEG);

			if (verbose) System.out.println("  "+mSOSSegment.getNumComponents()+" "+mJPEG.comps_in_scan+" "+mJPEG.blocks_in_MCU);

			int[][] mcu = new int[mJPEG.blocks_in_MCU][64];
			int compIndex = mSOSSegment.getComponent(0) - 1;

			try
			{
				if (mSOSSegment.getNumComponents() == 1)
				{
					ComponentInfo comp = mJPEG.cur_comp_info[0];
					int samplingX = comp.getHorSampleFactor();
					int samplingY = comp.getVerSampleFactor();

					for (int loop = 0; mJPEG.unread_marker==0; loop++)
					{
						for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
						{
							for (int blockY = 0; blockY < samplingY; blockY++)
							{
								for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
								{
									for (int blockX = 0; blockX < samplingX; blockX++)
									{
										mDecoder.decode_mcu(mJPEG, mcu);
										accumBuffer(mcu[0], mDctCoefficients[mcuY][mcuX][blockY][blockX][compIndex]);
									}
								}
							}
						}
					}
				}
				else
				{
					for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
					{
						for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
						{
							mDecoder.decode_mcu(mJPEG, mcu);

							for (int component = 0, blockIndex = 0; component < mSOSSegment.getNumComponents(); component++)
							{
								ComponentInfo comp = mJPEG.cur_comp_info[component];
								int samplingX = comp.getHorSampleFactor();
								int samplingY = comp.getVerSampleFactor();

								for (int blockY = 0; blockY < samplingY; blockY++)
								{
									for (int blockX = 0; blockX < samplingX; blockX++, blockIndex++)
									{
										accumBuffer(mcu[blockIndex], mDctCoefficients[mcuY][mcuX][blockY][blockX][component]);
									}
								}
							}
						}
					}
				}
			}
			catch (Throwable e)
			{
				e.printStackTrace(System.out);
				mStop = true;
			}

			if (verbose) debugprint(30,30);

			if (mStop || !mSOFSegment.isProgressive() || mProgressiveLevel++ == 99 || mJPEG.unread_marker == 217)
			{
				if (mSOFSegment.isProgressive() && mJPEG.unread_marker != 217)
				{
					hexdump();
				}

				mStop = true;

				int[][][][][][] dummy = mDctCoefficients.clone();

				for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
				{
					for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
					{
						for (int component = 0; component < mJPEG.num_components; component++)
						{
							ComponentInfo comp = mSOFSegment.getComponent(component);
							QuantizationTable quantizationTable = mJPEG.mQuantizationTables[comp.getQuantizationTableId()];
							int samplingX = comp.getHorSampleFactor();
							int samplingY = comp.getVerSampleFactor();

							for (int blockY = 0; blockY < samplingY; blockY++)
							{
								for (int blockX = 0; blockX < samplingX; blockX++)
								{
									idct.transform(dummy[mcuY][mcuX][blockY][blockX][component], quantizationTable);
								}
							}
						}
					}

					int[][] buffer = new int[3][maxSamplingX * maxSamplingY * 8 * 8];

					for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
					{
						for (int component = 0; component < mJPEG.num_components; component++)
						{
							ComponentInfo comp = mSOFSegment.getComponent(component);
							int samplingX = comp.getHorSampleFactor();
							int samplingY = comp.getVerSampleFactor();

							for (int blockY = 0; blockY < samplingY; blockY++)
							{
								for (int blockX = 0; blockX < samplingX; blockX++)
								{
									mImage.setData(blockX, blockY, samplingX, samplingY, dummy[mcuY][mcuX][blockY][blockX][component], buffer[component]);
								}
							}
						}

						mImage.flushMCU(mcuX, mcuY, buffer);
					}
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			mImage.setDamaged();
			throw e;
		}

		mBitStream.align();
	}



	public void debugprint(int aMcuX, int aMcuY)
	{
		printTables(new int[][]{mDctCoefficients[aMcuY][aMcuX][0][0][0], mDctCoefficients[aMcuY][aMcuX][0][1][0], mDctCoefficients[aMcuY][aMcuX][1][0][0], mDctCoefficients[aMcuY][aMcuX][1][1][0], mDctCoefficients[aMcuY][aMcuX][0][0][1], mDctCoefficients[aMcuY][aMcuX][0][0][2]});
		System.out.println();
	}


	//				if (mRestartInterval > 0 && (((mcuIndex += numHorMCU) % mRestartInterval) == 0))
	//				{
	//					if (mcuIndex < numHorMCU * numVerMCU - 1) // Don't check restart marker when all MCUs are loaded
	//					{
	//						mBitStream.align();
	//
	//						int restartMarker = mBitStream.readInt16();
	//						if (restartMarker != 0xFFD0 + (restartMarkerIndex & 7))
	//						{
	//							throw new IOException("Error reading JPEG stream; Expected restart marker " + Integer.toHexString(0xFFD0 + (restartMarkerIndex & 7)));
	//						}
	//						restartMarkerIndex++;
	//
	//						for (int i = 0; i < MAX_CHANNELS; i++)
	//						{
	//							mPreviousDCValue[i] = 0;
	//						}
	//					}
	//				}


	private void accumBuffer(int[] aSrc, int[] aDst)
	{
		for (int i = 0; i < 64; i++)
		{
			aDst[i] += aSrc[i];
		}
	}


	public void hexdump() throws IOException
	{
//		int streamOffset = mBitStream.getStreamOffset();
//
//		int cnt = 0;
//		int b1 = 0;
//		for (int r = 0; r < 1000; r++)
//		{
//			for (int c = 0; c < 64; c++, cnt++)
//			{
//				int b0 = mBitStream.readInt8();
//				System.out.printf("%02x ", b0);
//
//				if (b1 == 255 && b0 != 0)
//				{
//					System.out.println();
//					System.out.println("=> "+streamOffset+" +" + cnt + " ("+Integer.toHexString(streamOffset)+")");
//					return;
//				}
//
//				b1 = b0;
//				if ((c % 8) == 7) System.out.print(" ");
//			}
//			System.out.println();
//		}
	}
}
