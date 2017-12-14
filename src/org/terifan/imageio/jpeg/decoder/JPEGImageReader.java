package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.SOSSegment;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DQTSegment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP14Segment;
import org.terifan.imageio.jpeg.ColorSpace;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.JPEGImage;
import org.terifan.imageio.jpeg.QuantizationTable;


public class JPEGImageReader
{
	final static int MAX_CHANNELS = 3;

	private BitInputStream mBitStream;
	private int mRestartInterval;
	private SOFSegment mSOFSegment;
	private SOSSegment mSOSSegment;
	private Class<? extends IDCT> mIDCT;
	private boolean mStop;
	private JPEGImage mImage;
	private Decoder mDecoder;
	private int mProgressiveLevel;
	private JPEG mJPEG = new JPEG();
	private boolean mUpdateImage;


	private JPEGImageReader(InputStream aInputStream, Class<? extends IDCT> aIDCT) throws IOException
	{
		mBitStream = new BitInputStream(aInputStream);
		mIDCT = aIDCT;
		mUpdateImage = true;
	}


	public static JPEG decode(InputStream aInputStream) throws IOException
	{
		JPEGImageReader reader = new JPEGImageReader(aInputStream, IDCTFloat.class);
		reader.mUpdateImage = false;
		reader.readImpl();
		return reader.mJPEG;
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
						System.out.println("######### Bad input #########");

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
						mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF1: // Extended sequential, Huffman
						throw new IOException("Image encoding not supported.");
					case SOF2: // Progressive, Huffman
						throw new IOException("Image encoding not supported.");
					case SOF9: // Extended sequential, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
						mBitStream.setHandleEscapeChars(false);
						mJPEG.mArithmetic = true;
						mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF10: // Progressive, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
						mBitStream.setHandleEscapeChars(false);
						mJPEG.mArithmetic = true;
						mJPEG.mProgressive = true;
						mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
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
						mSOSSegment = new SOSSegment(mJPEG).read(mBitStream);
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

		int maxSamplingX = mSOFSegment.getMaxHorSampling();
		int maxSamplingY = mSOFSegment.getMaxVerSampling();
		int numHorMCU = mSOFSegment.getHorMCU();
		int numVerMCU = mSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		for (int scanComponentIndex = 0, first = 0; scanComponentIndex < mJPEG.num_components; scanComponentIndex++)
		{
			ComponentInfo comp = mSOFSegment.getComponent(scanComponentIndex);
			comp.setComponentBlockOffset(first);
			first += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		mJPEG.blocks_in_MCU = 0;

		for (int scanComponentIndex = 0; scanComponentIndex < mJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = mSOFSegment.getComponentByScan(mSOSSegment.getComponent(scanComponentIndex));
			mJPEG.blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		mJPEG.MCU_membership = new int[mJPEG.blocks_in_MCU];
		mJPEG.cur_comp_info = new ComponentInfo[mJPEG.comps_in_scan];

		for (int scanComponentIndex = 0, blockIndex = 0; scanComponentIndex < mJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = mSOFSegment.getComponentByScan(mSOSSegment.getComponent(scanComponentIndex));
			comp.setTableAC(mSOSSegment.getACTable(scanComponentIndex));
			comp.setTableDC(mSOSSegment.getDCTable(scanComponentIndex));

			mJPEG.cur_comp_info[scanComponentIndex] = comp;

			for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, blockIndex++)
			{
				mJPEG.MCU_membership[blockIndex] = scanComponentIndex;
			}

			System.out.println("  SOF: "+comp);
		}

		if (mImage == null)
		{
			mJPEG.mCoefficients = new int[numVerMCU][numHorMCU][mJPEG.blocks_in_MCU][64];

			mDecoder.jinit_decoder(mJPEG);

			mImage = new JPEGImage(mSOFSegment.getWidth(), mSOFSegment.getHeight(), maxSamplingX, maxSamplingY, mJPEG.num_components);
		}

		mDecoder.start_pass(mJPEG);

		try
		{
			int[][] mcu = new int[mJPEG.blocks_in_MCU][64];

			if (mJPEG.comps_in_scan == 1)
			{
				ComponentInfo comp = mJPEG.cur_comp_info[0];
				int componentBlockOffset = comp.getComponentBlockOffset();

				for (int loop = 0; mJPEG.unread_marker==0; loop++)
				{
					for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
					{
						for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
						{
							for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
							{
								for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
								{
									mDecoder.decode_mcu(mJPEG, mcu);
									addBlocks(mcu[0], mJPEG.mCoefficients[mcuY][mcuX][componentBlockOffset + comp.getHorSampleFactor() * blockY + blockX]);
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

						for (int blockIndex = 0; blockIndex < mJPEG.blocks_in_MCU; blockIndex++)
						{
							addBlocks(mcu[blockIndex], mJPEG.mCoefficients[mcuY][mcuX][blockIndex]);
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

		if (mStop || !mJPEG.mProgressive || mProgressiveLevel++ == 99 || mJPEG.unread_marker == 217)
		{
			mStop = true;

			if (mUpdateImage)
			{
				updateImage(numVerMCU, numHorMCU, idct, maxSamplingX, maxSamplingY, mcuWidth, mcuHeight);
			}
		}

		mBitStream.align();
	}


	private void updateImage(int aNumVerMCU, int aNumHorMCU, IDCT aIdct, int aMaxSamplingX, int aMaxSamplingY, int mcuWidth, int mcuHeight)
	{
		int[] blockLookup = {0,0,0,0,1,2};
//		int[] blockLookup = {0,1,2};

		for (int mcuY = 0; mcuY < aNumVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < aNumHorMCU; mcuX++)
			{
				for (int blockIndex = 0; blockIndex < blockLookup.length; blockIndex++)
				{
					ComponentInfo comp = mSOFSegment.getComponent(blockLookup[blockIndex]);

					QuantizationTable quantizationTable = mJPEG.mQuantizationTables[comp.getQuantizationTableId()];

					aIdct.transform(mJPEG.mCoefficients[mcuY][mcuX][blockIndex], quantizationTable);
				}

				for (int blockY = 0; blockY < aMaxSamplingY; blockY++)
				{
					for (int blockX = 0; blockX < aMaxSamplingX; blockX++)
					{
						for (int y = 0; y < 8; y++)
						{
							for (int x = 0; x < 8; x++)
							{
								// 4:4:4
//								int lu = mJPEG.mCoefficients[mcuY][mcuX][0][y * 8 + x];
//								int cb = mJPEG.mCoefficients[mcuY][mcuX][1][y * 8 + x];
//								int cr = mJPEG.mCoefficients[mcuY][mcuX][2][y * 8 + x];

								// 4:2:0
								int lu = mJPEG.mCoefficients[mcuY][mcuX][blockY * 2 + blockX][y * 8 + x];
								int cb = mJPEG.mCoefficients[mcuY][mcuX][4][8 * blockY * 4 + y / 2 * 8 + x / 2 + 4 * blockX];
								int cr = mJPEG.mCoefficients[mcuY][mcuX][5][8 * blockY * 4 + y / 2 * 8 + x / 2 + 4 * blockX];

								int rx = mcuX * mcuWidth + 8 * blockX + x;
								int ry = y + mcuY * mcuHeight + 8 * blockY;

								if (rx < mSOFSegment.getWidth() && ry < mSOFSegment.getHeight())
								{
									mImage.getRaster()[ry * mSOFSegment.getWidth() + rx] = ColorSpace.yuvToRgbFP(lu, cb, cr);
								}
							}
						}
					}
				}
			}
		}
	}


	private void addBlocks(int[] aSrc, int[] aDst)
	{
		for (int i = 0; i < 64; i++)
		{
			aDst[i] += aSrc[i];
		}
	}


//	public void debugprint(int aMcuX, int aMcuY)
//	{
//		printTables(new int[][]{mDctCoefficients[aMcuY][aMcuX][0][0][0], mDctCoefficients[aMcuY][aMcuX][0][1][0], mDctCoefficients[aMcuY][aMcuX][1][0][0], mDctCoefficients[aMcuY][aMcuX][1][1][0], mDctCoefficients[aMcuY][aMcuX][0][0][1], mDctCoefficients[aMcuY][aMcuX][0][0][2]});
//		System.out.println();
//	}


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
