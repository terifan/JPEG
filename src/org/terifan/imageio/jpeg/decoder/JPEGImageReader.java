package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.JPEGImage;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.SOSMarkerSegment;
import org.terifan.imageio.jpeg.SOFMarkerSegment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DHTMarkerSegment;
import org.terifan.imageio.jpeg.DQTMarkerSegment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import org.terifan.imageio.jpeg.ColorSpace;
import org.terifan.imageio.jpeg.ColorSpace.ColorSpaceType;
import static org.terifan.imageio.jpeg.JPEGConstants.*;


public class JPEGImageReader extends JPEGConstants
{
	final static int MAX_CHANNELS = 3;

	private BitInputStream mBitStream;
	private final DQTMarkerSegment[] mQuantizationTables;
	private int mRestartInterval;
	private SOFMarkerSegment mSOFMarkerSegment;
	private SOSMarkerSegment mSOSMarkerSegment;
	private int mDensitiesUnits;
	private int mDensityX;
	private int mDensityY;
	private Class<? extends IDCT> mIDCT;
	private ColorSpaceType mColorSpace;
	private boolean mStop;
	private JPEGImage mImage;
	private int[][][][][][] mDctCoefficients;
	private Decoder mDecoder;
	private int mProgressiveLevel;
	private DecompressionState cinfo = new DecompressionState();


	private JPEGImageReader(InputStream aInputStream, Class<? extends IDCT> aIDCT) throws IOException
	{
		mQuantizationTables = new DQTMarkerSegment[MAX_CHANNELS];

		mBitStream = new BitInputStream(aInputStream);
		mIDCT = aIDCT;
	}


	private void readDACMarkerSegment() throws IOException
	{
		int length = mBitStream.readInt16() - 2;

		while (length > 0)
		{
			int index = mBitStream.readInt8();
			int val = mBitStream.readInt8();
			length -= 2;

			if (index < 0 || index >= (2 * NUM_ARITH_TBLS))
			{
				throw new IllegalArgumentException("Bad DAC index: " + index);
			}

			if (index >= NUM_ARITH_TBLS) // define AC table
			{
//				System.out.println("  arith_ac_K[" + (index - NUM_ARITH_TBLS) + "]=" + val);

				cinfo.arith_ac_K[index - NUM_ARITH_TBLS] = val;
			}
			else // define DC table
			{
				cinfo.arith_dc_U[index] = val >> 4;
				cinfo.arith_dc_L[index] = val & 0x0F;

//				System.out.println("  arith_dc_L[" + index + "]=" + cinfo.arith_dc_L[index]);
//				System.out.println("  arith_dc_U[" + index + "]=" + cinfo.arith_dc_U[index]);

				if (cinfo.arith_dc_L[index] > cinfo.arith_dc_U[index])
				{
					throw new IllegalArgumentException("Bad DAC value: " + val);
				}
			}
		}

		if (length != 0)
		{
			throw new IllegalArgumentException("Bad DAC segment: remaining: " + length);
		}

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
				if (cinfo != null && cinfo.unread_marker != 0)
				{
					nextSegment = 0xff00 + cinfo.unread_marker;
					cinfo.unread_marker = 0;
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
						readAPPSegmentMarker();
						break;
					case APP14:
						readAPP14SegmentMarker();
						break;
					case DQT:
					{
						int segmentLength = mBitStream.readInt16() - 2;
						do
						{
							DQTMarkerSegment temp = new DQTMarkerSegment(mBitStream);
							mQuantizationTables[temp.getIdentity()] = temp;
							segmentLength -= 1 + temp.getPrecision() * 64;
							if (segmentLength < 0)
							{
								throw new IOException("Error in JPEG stream; illegal DQT segment size.");
							}
						}
						while (segmentLength > 0);
						break;
					}
					case DHT:
						((HuffmanDecoder)mDecoder).readHuffmanTables();
						break;
					case DAC: // Arithmetic Table
						readDACMarkerSegment();
						break;
					case SOF0: // Baseline
						mDecoder = new HuffmanDecoder(mBitStream);
						mBitStream.setHandleEscapeChars(true);
						mSOFMarkerSegment = new SOFMarkerSegment(mBitStream, false, false);
						break;
					case SOF1: // Extended sequential, Huffman
						throw new IOException("Image encoding not supported.");
					case SOF2: // Progressive, Huffman
						throw new IOException("Image encoding not supported.");
					case SOF9: // Extended sequential, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
						mBitStream.setHandleEscapeChars(false);
						mSOFMarkerSegment = new SOFMarkerSegment(mBitStream, true, false);
						break;
					case SOF10: // Progressive, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
						mBitStream.setHandleEscapeChars(false);
						mSOFMarkerSegment = new SOFMarkerSegment(mBitStream, true, true);
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
						mSOSMarkerSegment = new SOSMarkerSegment(mBitStream);
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


	private void readAPP14SegmentMarker() throws IOException
	{
		int offset = mBitStream.getStreamOffset();
		int length = mBitStream.readInt16();

		if (mBitStream.readInt8() == 'A' && mBitStream.readInt8() == 'd' && mBitStream.readInt8() == 'o' && mBitStream.readInt8() == 'b' && mBitStream.readInt8() == 'e')
		{
			if (mBitStream.readInt8() != 100)
			{
				mBitStream.skipBytes(1); //flags 0
				mBitStream.skipBytes(1); //flags 1

				switch (mBitStream.readInt8())
				{
					case 1:
						mColorSpace = ColorSpace.ColorSpaceType.YCBCR;
						break;
					case 2:
						mColorSpace = ColorSpace.ColorSpaceType.YCCK;
						break;
					default:
						mColorSpace = ColorSpace.ColorSpaceType.RGB;
						break;
				}
			}
		}

		if (mBitStream.getStreamOffset() != offset + length)
		{
			throw new IOException("Expected offset " + (offset + length) + ", actual " + mBitStream.getStreamOffset());
		}
	}


	private void readAPPSegmentMarker() throws IOException
	{
		int segmentLength = mBitStream.readInt16();

		StringBuilder segmentType = new StringBuilder();
		for (int c; (c = mBitStream.readInt8()) != 0;)
		{
			segmentType.append((char)c);
		}

		switch (segmentType.toString())
		{
			case "JFIF":
				int majorVersion = mBitStream.readInt8();
				int minorVersion = mBitStream.readInt8();
				if (majorVersion != 1)
				{
					throw new IOException("Error in JPEG stream; unsupported version: " + majorVersion + "." + minorVersion);
				}
				mDensitiesUnits = mBitStream.readInt8();
				mDensityX = mBitStream.readInt16();
				mDensityY = mBitStream.readInt16();
				int thumbnailSize = mBitStream.readInt8() * mBitStream.readInt8() * 3; // thumbnailWidth, thumbnailHeight
				if (segmentLength != 16 + thumbnailSize)
				{
					throw new IOException("Error in JPEG stream; illegal APP0 segment size.");
				}
				mBitStream.skipBytes(thumbnailSize); // uncompressed 24-bit thumbnail raster
				if (VERBOSE)
				{
					System.out.println("Ignoring thumbnail " + thumbnailSize + " bytes");
				}
				break;
			case "JFXX":
				int extensionCode = mBitStream.readInt8();
				switch (extensionCode)
				{
					case 0x10: // jpeg encoded
					case 0x11: // 8-bit palette
					case 0x13: // 24-bit RGB
				}
				mBitStream.skipBytes(segmentLength - 8);
				if (VERBOSE)
				{
					System.out.println("Ignoring thumbnail " + (segmentLength-8) + " bytes");
				}
				break;
			default:
				throw new IOException("Unsupported APP0 extension: " + segmentType);
		}
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
			cinfo.Ss = mSOSMarkerSegment.getSs();
			cinfo.Se = mSOSMarkerSegment.getSe();
			cinfo.Ah = mSOSMarkerSegment.getAh();
			cinfo.Al = mSOSMarkerSegment.getAl();
			cinfo.comps_in_scan = mSOSMarkerSegment.getNumComponents();
			cinfo.num_components = mSOFMarkerSegment.getNumComponents();
			cinfo.progressive_mode = mSOFMarkerSegment.isProgressive();
			cinfo.lim_Se = DCTSIZE2-1;

			cinfo.blocks_in_MCU = 0;

			for (int scanComponentIndex = 0, j = 0; scanComponentIndex < mSOSMarkerSegment.getNumComponents(); scanComponentIndex++)
			{
				for (int frameComponentIndex = 0; frameComponentIndex < mSOFMarkerSegment.getNumComponents(); frameComponentIndex++)
				{
					if (mSOFMarkerSegment.getComponent(frameComponentIndex).getComponentIndex() == mSOSMarkerSegment.getComponent(scanComponentIndex))
					{
						ComponentInfo comp = mSOFMarkerSegment.getComponent(frameComponentIndex);

						cinfo.blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
					}
				}
			}

			int maxSamplingX = 0;
			int maxSamplingY = 0;
			for (int i = 0; i < mSOFMarkerSegment.getNumComponents(); i++)
			{
				ComponentInfo comp = mSOFMarkerSegment.getComponent(i);
				maxSamplingX = Math.max(maxSamplingX, comp.getHorSampleFactor());
				maxSamplingY = Math.max(maxSamplingY, comp.getVerSampleFactor());
			}

			int numHorMCU = (mSOFMarkerSegment.getWidth() + 8 * maxSamplingX - 1) / (8 * maxSamplingX);
			int numVerMCU = (mSOFMarkerSegment.getHeight() + 8 * maxSamplingY - 1) / (8 * maxSamplingY);

			cinfo.MCU_membership = new int[cinfo.blocks_in_MCU];
			cinfo.cur_comp_info = new ComponentInfo[cinfo.num_components];

			for (int scanComponentIndex = 0, j = 0; scanComponentIndex < mSOSMarkerSegment.getNumComponents(); scanComponentIndex++)
			{
				for (int frameComponentIndex = 0; frameComponentIndex < mSOFMarkerSegment.getNumComponents(); frameComponentIndex++)
				{
					if (mSOFMarkerSegment.getComponent(frameComponentIndex).getComponentIndex() == mSOSMarkerSegment.getComponent(scanComponentIndex))
					{
						ComponentInfo comp = mSOFMarkerSegment.getComponent(frameComponentIndex);
						comp.setTableAC(mSOSMarkerSegment.getACTable(scanComponentIndex));
						comp.setTableDC(mSOSMarkerSegment.getDCTable(scanComponentIndex));

						cinfo.cur_comp_info[scanComponentIndex] = comp;

						for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, j++)
						{
							cinfo.MCU_membership[j] = scanComponentIndex;
						}

						System.out.println("  SOF: "+comp);
					}
				}
			}

			if (mImage == null)
			{
				mDctCoefficients = new int[numVerMCU][numHorMCU][maxSamplingY][maxSamplingX][mSOFMarkerSegment.getNumComponents()][64];

				mDecoder.jinit_decoder(cinfo);

				mImage = new JPEGImage(mSOFMarkerSegment.getWidth(), mSOFMarkerSegment.getHeight(), maxSamplingX, maxSamplingY, mDensitiesUnits, mDensityX, mDensityY, mSOFMarkerSegment.getNumComponents());
			}

			mDecoder.start_pass(cinfo);

			if (verbose) System.out.println("  "+mSOSMarkerSegment.getNumComponents()+" "+cinfo.comps_in_scan+" "+cinfo.blocks_in_MCU);

			int[][] mcu = new int[cinfo.blocks_in_MCU][64];
			int compIndex = mSOSMarkerSegment.getComponent(0) - 1;

			try
			{
				if (mSOSMarkerSegment.getNumComponents() == 1)
				{
					ComponentInfo comp = cinfo.cur_comp_info[0];
					int samplingX = comp.getHorSampleFactor();
					int samplingY = comp.getVerSampleFactor();

					for (int loop = 0; cinfo.unread_marker==0; loop++)
					{
						for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
						{
							for (int blockY = 0; blockY < samplingY; blockY++)
							{
								for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
								{
									for (int blockX = 0; blockX < samplingX; blockX++)
									{
										mDecoder.decode_mcu(cinfo, mcu);
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
							mDecoder.decode_mcu(cinfo, mcu);

							for (int component = 0, blockIndex = 0; component < mSOSMarkerSegment.getNumComponents(); component++)
							{
								ComponentInfo comp = cinfo.cur_comp_info[component];
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
				e.printStackTrace(System.err);
				mStop = true;
			}

			if (verbose) debugprint(30,30);

			if (mStop || !mSOFMarkerSegment.isProgressive() || mProgressiveLevel++ == 99 || cinfo.unread_marker == 217)
			{
				if (mSOFMarkerSegment.isProgressive() && cinfo.unread_marker != 217)
				{
					hexdump();
				}

				mStop = true;

				int[][][][][][] dummy = mDctCoefficients.clone();

				for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
				{
					for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
					{
						for (int component = 0; component < mSOFMarkerSegment.getNumComponents(); component++)
						{
							ComponentInfo comp = mSOFMarkerSegment.getComponent(component);
							DQTMarkerSegment quantizationTable = mQuantizationTables[comp.getQuantizationTableId()];
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
						for (int component = 0; component < mSOFMarkerSegment.getNumComponents(); component++)
						{
							ComponentInfo comp = mSOFMarkerSegment.getComponent(component);
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



	//					if (!readDCTCofficients(dctCoefficients[x], numComponents))
	//					{
	//						if (mcuCounter == 0)
	//						{
	//							throw new IOException("Error reading JPEG stream; Failed to decode Huffman code.");
	//						}
	//						image.setDamaged();
	//						return image;
	//					}

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


	private static void printTables(int[][] aInput)
	{
		for (int r = 0; r < 8; r++)
		{
			for (int t = 0; t < aInput.length; t++)
			{
				for (int c = 0; c < 8; c++)
				{
					System.out.printf("%5d ", aInput[t][r*8+c]);
				}
				System.out.print(r == 4 && t < aInput.length-1 ? "  ===>" : "      ");
			}
			System.out.println();
		}
	}


	private void accumBuffer(int[] aSrc, int[] aDst)
	{
		for (int i = 0; i < 64; i++)
		{
			aDst[i] += aSrc[i];
		}
	}


	public void hexdump() throws IOException
	{
		int streamOffset = mBitStream.getStreamOffset();

		int cnt = 0;
		int b1 = 0;
		for (int r = 0; r < 1000; r++)
		{
			for (int c = 0; c < 64; c++, cnt++)
			{
				int b0 = mBitStream.readInt8();
				System.out.printf("%02x ", b0);

				if (b1 == 255 && b0 != 0)
				{
					System.out.println();
					System.out.println("=> "+streamOffset+" +" + cnt + " ("+Integer.toHexString(streamOffset)+")");
					return;
				}

				b1 = b0;
				if ((c % 8) == 7) System.out.print(" ");
			}
			System.out.println();
		}
	}
}