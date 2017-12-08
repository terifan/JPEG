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
import java.util.Arrays;
import org.terifan.imageio.jpeg.DACMarkerSegment;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.decoder.ArithmeticDecoder.JBLOCKROW;


public class JPEGImageReader extends JPEGConstants
{
	final static int MAX_CHANNELS = 3;

	private BitInputStream mBitStream;
	private final DHTMarkerSegment[][] mHuffmanTables;
	private final DQTMarkerSegment[] mQuantizationTables;
	private final int[] mPreviousDCValue;
	private int mRestartInterval;
	private SOFMarkerSegment mSOFMarkerSegment;
	private SOSMarkerSegment mSOSMarkerSegment;
	private int mDensitiesUnits;
	private int mDensityX;
	private int mDensityY;
	private Class<? extends IDCT> mDecoder;
	private DACMarkerSegment mDACMarkerSegment;

	private j_decompress_ptr cinfo = new j_decompress_ptr();
	

	private JPEGImageReader(InputStream aInputStream, Class<? extends IDCT> aDecoder) throws IOException
	{
		mPreviousDCValue = new int[MAX_CHANNELS];
		mQuantizationTables = new DQTMarkerSegment[MAX_CHANNELS];
		mHuffmanTables = new DHTMarkerSegment[MAX_CHANNELS][2];

		mBitStream = new BitInputStream(aInputStream);
		this.mDecoder = aDecoder;
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

			for (;;)
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
					{
						int segmentLength = mBitStream.readInt16() - 2;
						do
						{
							DHTMarkerSegment temp = new DHTMarkerSegment(mBitStream);
							mHuffmanTables[temp.getIdentity()][temp.getType()] = temp;
							segmentLength -= 17 + temp.getNumSymbols();
							if (segmentLength < 0)
							{
								throw new IOException("Error in JPEG stream; illegal DHT segment size.");
							}
						}
						while (segmentLength > 0);
						break;
					}
					case DAC: // Arithmetic Table
						mDACMarkerSegment = new DACMarkerSegment(mBitStream, cinfo);
						break;
					case SOF0: // Baseline
						mSOFMarkerSegment = new SOFMarkerSegment(mBitStream, false, false);
						break;
					case SOF1: // Extended sequential, Huffman
						throw new IOException("Image encoding not supported.");
					case SOF2: // Progressive, Huffman
						throw new IOException("Image encoding not supported.");
					case SOF9: // Extended sequential, arithmetic
						mSOFMarkerSegment = new SOFMarkerSegment(mBitStream, true, false);
						break;
					case SOF10: // Progressive, arithmetic
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


	public void hexdump() throws IOException
	{
		for (int r = 0; r < 10; r++)
		{
			for (int c = 0; c < 64; c++)
			{
				System.out.printf("%02x ", mBitStream.readInt8());
			}
			System.out.println();
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


	JPEGImage mImage;
	int[][][][][][] dctCoefficients;
	ArithmeticDecoder ariDecoder;
	int progLevel;

	private void readRaster() throws IOException
	{
		IDCT idct;
		try
		{
			idct = mDecoder.newInstance();
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}

		int maxSamplingX = mSOFMarkerSegment.getMaxSamplingX();
		int maxSamplingY = mSOFMarkerSegment.getMaxSamplingY();
		int numHorMCU = (int)Math.ceil(mSOFMarkerSegment.getWidth() / (8.0 * maxSamplingX));
		int numVerMCU = (int)Math.ceil(mSOFMarkerSegment.getHeight() / (8.0 * maxSamplingY));
		int restartMarkerIndex = 0;

		try
		{
			cinfo.Ss = mSOSMarkerSegment.getSs();
			cinfo.Se = mSOSMarkerSegment.getSe();
			cinfo.Ah = mSOSMarkerSegment.getAh();
			cinfo.Al = mSOSMarkerSegment.getAl();
			cinfo.comps_in_scan = mSOSMarkerSegment.getNumComponents();
			cinfo.num_components = mSOSMarkerSegment.getNumComponents();

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

			cinfo.MCU_membership = new int[cinfo.blocks_in_MCU];
			cinfo.cur_comp_info = new ComponentInfo[cinfo.num_components];

			for (int scanComponentIndex = 0, j = 0; scanComponentIndex < mSOSMarkerSegment.getNumComponents(); scanComponentIndex++)
			{
				for (int frameComponentIndex = 0; frameComponentIndex < mSOFMarkerSegment.getNumComponents(); frameComponentIndex++)
				{
					if (mSOFMarkerSegment.getComponent(frameComponentIndex).getComponentIndex() == mSOSMarkerSegment.getComponent(scanComponentIndex))
					{
						ComponentInfo comp = mSOFMarkerSegment.getComponent(frameComponentIndex);
						cinfo.cur_comp_info[scanComponentIndex] = comp;

						for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, j++)
						{
							cinfo.MCU_membership[j] = scanComponentIndex;
						}

						System.out.println(comp.getHorSampleFactor()+" * "+comp.getVerSampleFactor());
					}
				}
			}

			cinfo.natural_order = NATURAL_ORDER;
			cinfo.progressive_mode = mSOFMarkerSegment.isProgressive();
			cinfo.lim_Se = DCTSIZE2-1;

			if (mImage == null)
			{
				dctCoefficients = new int[numVerMCU][numHorMCU][maxSamplingY][maxSamplingX][3][64];

				ariDecoder = new ArithmeticDecoder(mBitStream);
				ariDecoder.jinit_arith_decoder(cinfo);

				mImage = new JPEGImage(mSOFMarkerSegment.getWidth(), mSOFMarkerSegment.getHeight(), maxSamplingX, maxSamplingY, mDensitiesUnits, mDensityX, mDensityY, mSOFMarkerSegment.getNumComponents());
			}

			ariDecoder.start_pass(cinfo);

			System.out.println(mSOSMarkerSegment.getNumComponents()+" "+cinfo.comps_in_scan+" "+cinfo.blocks_in_MCU);

			JBLOCKROW[] mcu = new JBLOCKROW[cinfo.blocks_in_MCU];
			for (int i = 0; i < cinfo.blocks_in_MCU; i++)
			{
				mcu[i] = new JBLOCKROW();
			}

			if (mSOSMarkerSegment.getNumComponents() == 1 && mSOSMarkerSegment.getComponent(0) == ComponentInfo.Y)
			{
				for (int mcuY = 0, mcuIndex = 0; mcuY < numVerMCU; mcuY++)
				{
					for (int mcuRow = 0; mcuRow < 2; mcuRow++)
					{
						for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
						{
							if (mcuRow == 0)
							{
								ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][0][0][0]);
								ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][0][1][0]);
							}
							else
							{
								ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][1][0][0]);
								ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][1][1][0]);
							}
							
							if (mcuRow == 1 && (mcuX == 0 && (mcuY == 0 || mcuY == 10)))
							{
								printTables(new int[][]{dctCoefficients[mcuY][mcuX][0][0][0], dctCoefficients[mcuY][mcuX][0][1][0], dctCoefficients[mcuY][mcuX][1][0][0], dctCoefficients[mcuY][mcuX][1][1][0], dctCoefficients[mcuY][mcuX][0][0][1], dctCoefficients[mcuY][mcuX][0][0][2]});
								System.out.println();
							}
						}
					}
				}
			}
else
			{

			for (int mcuY = 0, mcuIndex = 0; mcuY < numVerMCU; mcuY++)
			{
				for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
				{
					if (mSOSMarkerSegment.getNumComponents() == 1)
					{
						if (mSOSMarkerSegment.getComponent(0) == ComponentInfo.CB)
						{
							ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][0][0][1]);
						}
						else if (mSOSMarkerSegment.getComponent(0) == ComponentInfo.CR)
						{
							ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][0][0][2]);
						}
						else
						{
							ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][0][0][0]);
							ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][0][1][0]);
							ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][1][0][0]);
							ariDecoder.decode_mcu(cinfo, mcu); arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][1][1][0]);
						}
					}
					else
					{
						ariDecoder.decode_mcu(cinfo, mcu);

						arraycopy(mcu[0].data, dctCoefficients[mcuY][mcuX][0][0][0]);
						arraycopy(mcu[1].data, dctCoefficients[mcuY][mcuX][0][1][0]);
						arraycopy(mcu[2].data, dctCoefficients[mcuY][mcuX][1][0][0]);
						arraycopy(mcu[3].data, dctCoefficients[mcuY][mcuX][1][1][0]);

						if (cinfo.blocks_in_MCU > 4)
						{
							arraycopy(mcu[4].data, dctCoefficients[mcuY][mcuX][0][0][1]);
							arraycopy(mcu[5].data, dctCoefficients[mcuY][mcuX][0][0][2]);
						}
					}

					if (mcuX == 0 && (mcuY == 0 || mcuY == 10))
					{
//						printTables(new int[][]{mcu[0].data,mcu[1].data,mcu[2].data,mcu[3].data});
						printTables(new int[][]{dctCoefficients[mcuY][mcuX][0][0][0], dctCoefficients[mcuY][mcuX][0][1][0], dctCoefficients[mcuY][mcuX][1][0][0], dctCoefficients[mcuY][mcuX][1][1][0], dctCoefficients[mcuY][mcuX][0][0][1], dctCoefficients[mcuY][mcuX][0][0][2]});
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
			}
			
}

			if (++progLevel==2)
			{
				{
				int mcuX = 0;
				int mcuY = 10;
				printTables(new int[][]{dctCoefficients[mcuY][mcuX][0][0][0], dctCoefficients[mcuY][mcuX][0][1][0], dctCoefficients[mcuY][mcuX][1][0][0], dctCoefficients[mcuY][mcuX][1][1][0], dctCoefficients[mcuY][mcuX][0][0][1], dctCoefficients[mcuY][mcuX][0][0][2]});
				}

				hexdump();

				int[][][][][][] dummy = dctCoefficients.clone();

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
			mImage.setDamaged();
			throw e;
		}

//		mBitStream.align();
	}


	private boolean readDCTCofficients(int[][][][] aCoefficients, int numComponents) throws IOException
	{
		for (int component = 0; component < numComponents; component++)
		{
			ComponentInfo comp = mSOFMarkerSegment.getComponent(component);
			int samplingX = comp.getHorSampleFactor();
			int samplingY = comp.getVerSampleFactor();

			for (int cy = 0; cy < samplingY; cy++)
			{
				for (int cx = 0; cx < samplingX; cx++)
				{
					if (!readDCTCofficients(aCoefficients[cy][cx][component], component))
					{
						return false;
					}
				}
			}
		}

		return true;
	}


	private boolean readDCTCofficients(int[] aCoefficients, int aComponent) throws IOException
	{
		Arrays.fill(aCoefficients, 0);

		DHTMarkerSegment dcTable = mHuffmanTables[mSOSMarkerSegment.getDCTable(aComponent)][0];
		DHTMarkerSegment acTable = mHuffmanTables[mSOSMarkerSegment.getACTable(aComponent)][1];

		int value = dcTable.decodeSymbol(mBitStream);

		if (value == -1)
		{
			return false;
		}

		if (value > 0)
		{
			mPreviousDCValue[aComponent] += dcTable.readCoefficient(mBitStream, value);
		}

		aCoefficients[ZIGZAG_ORDER[0]] = mPreviousDCValue[aComponent];

		for (int offset = 1; offset < 64; offset++)
		{
			value = acTable.decodeSymbol(mBitStream);

			if (value == -1)
			{
				return false;
			}

			int zeroCount = value >> 4;
			int codeLength = value & 15;

			offset += zeroCount;

			if (codeLength > 0)
			{
				aCoefficients[ZIGZAG_ORDER[offset]] = acTable.readCoefficient(mBitStream, codeLength);
			}
			else if (zeroCount == 0)
			{
				break; // EOB found.
			}
			else if (zeroCount != 15)
			{
				throw new IOException("Error reading JPEG stream; ZeroCount must be 0 or 15 when codeLength equals 0.");
			}
		}

		return true;
	}


	private static void printTables(int[][] aInput)
	{
		for (int r = 0; r < 8; r++)
		{
			for (int t = 0; t < aInput.length; t++)
			{
				for (int c = 0; c < 8; c++)
				{
					System.out.printf("%6d ", aInput[t][r*8+c]);
				}
				System.out.print(r == 4 && t < aInput.length-1 ? "  ===>" : "      ");
			}
			System.out.println();
		}
	}


	private void arraycopy(int[] aSrc, int[] aDst)
	{
		for (int i = 0; i < 64; i++)
		{
			aDst[i] += aSrc[i];

//			aDst[i] = aSrcDst[i] + aSrc[i];
//
//			aSrcDst[i] = aDst[i];
		}
	}
}