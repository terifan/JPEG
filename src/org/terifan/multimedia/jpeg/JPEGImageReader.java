package org.terifan.multimedia.jpeg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.terifan.multimedia.jpeg.SOFMarkerSegment.ComponentInfo;


public class JPEGImageReader extends JPEG
{
	final static int MAX_CHANNELS = 3;
	final static boolean VERBOSE = false;

	final static int[] ZIGZAG = new int[]
	{
		0, 1, 8, 16, 9, 2, 3, 10,
		17, 24, 32, 25, 18, 11, 4, 5,
		12, 19, 26, 33, 40, 48, 41, 34,
		27, 20, 13, 6, 7, 14, 21, 28,
		35, 42, 49, 56, 57, 50, 43, 36,
		29, 22, 15, 23, 30, 37, 44, 51,
		58, 59, 52, 45, 38, 31, 39, 46,
		53, 60, 61, 54, 47, 55, 62, 63
	};
	
	private BitInputStream mBitStream;
	private final DHTMarkerSegment[][] mHuffmanTables;
	private final DQTMarkerSegment[] mQuantizationTables;
	private final int[] mDCTCoefficients;
	private final int[] mPreviousDCValue;
	private int mRestartInterval;
	private SOFMarkerSegment mFrameSegment;
	private SOSMarkerSegment mScanSegment;
	private int mDensitiesUnits;
	private int mDensityX;
	private int mDensityY;


	private JPEGImageReader(InputStream aInputStream) throws IOException
	{
		mPreviousDCValue = new int[MAX_CHANNELS];
		mQuantizationTables = new DQTMarkerSegment[MAX_CHANNELS];
		mHuffmanTables = new DHTMarkerSegment[MAX_CHANNELS][2];
		mDCTCoefficients = new int[64];

		mBitStream = new BitInputStream(aInputStream);
	}

	
	public static JPEGImage read(InputStream aInputStream) throws IOException
	{
		return new JPEGImageReader(aInputStream).readImpl();
	}
	

	private JPEGImage readImpl() throws IOException
	{
		JPEGImage image = null;

		try
		{
			int nextSegment = mBitStream.readShort();

			if (VERBOSE)
			{
				System.out.println(Integer.toString(nextSegment, 16) + " " + getSOFDescription(nextSegment));
			}

			if (nextSegment != SOI) // Start Of Image
			{
				throw new IOException("Error in JPEG stream; expected SOI segment marker but found: " + Integer.toString(nextSegment, 16));
			}

			do
			{
				nextSegment = mBitStream.readShort();

				if (VERBOSE)
				{
					System.out.println(Integer.toString(nextSegment, 16) + " " + getSOFDescription(nextSegment));
				}

				if ((nextSegment >> 8) != 255)
				{
					throw new IOException("Error in JPEG stream; expected segment marker but found: " + Integer.toString(nextSegment, 16));
				}

				switch (nextSegment)
				{
					case APP0:
						readAPPSegmentMarker();
						break;
					case DQT:
					{
						int segmentLength = mBitStream.readShort() - 2;
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
						int segmentLength = mBitStream.readShort() - 2;
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
					case SOF0:
						mFrameSegment = new SOFMarkerSegment(mBitStream);
						break;
					case SOS:
					{
						mScanSegment = new SOSMarkerSegment(mBitStream);
						image = readRaster();
						if (image.isDamaged())
						{
							return image;
						}
						break;
					}
					case DRI:
						mBitStream.skip(2); // skip length
						mRestartInterval = mBitStream.readShort();
						break;
					case COM:
						mBitStream.skip(mBitStream.readShort() - 2);
						break;
					case EOI:
						break;
					case SOF2:
						throw new IOException("Progressive images not supported.");
					default:
						mBitStream.skip(mBitStream.readShort() - 2);
						break;
				}
			}
			while (nextSegment != EOI);
		}
		finally
		{
			mBitStream.close();
			mBitStream = null;
		}

		return image;
	}


	private void readAPPSegmentMarker() throws IOException
	{
		int segmentLength = mBitStream.readShort();

		StringBuilder segmentType = new StringBuilder();
		for (int c; (c = mBitStream.readByte()) != 0;)
		{
			segmentType.append((char)c);
		}

		switch (segmentType.toString())
		{
			case "JFIF":
				int majorVersion = mBitStream.readByte();
				int minorVersion = mBitStream.readByte();
				if (majorVersion != 1)
				{
					throw new IOException("Error in JPEG stream; unsupported version: " + majorVersion + "." + minorVersion);
				}
				mDensitiesUnits = mBitStream.readByte();
				mDensityX = mBitStream.readShort();
				mDensityY = mBitStream.readShort();
				int thumbnailSize = mBitStream.readByte() * mBitStream.readByte() * 3; // thumbnailWidth, thumbnailHeight
				if (segmentLength != 16 + thumbnailSize)
				{
					throw new IOException("Error in JPEG stream; illegal APP0 segment size.");
				}
				mBitStream.skip(thumbnailSize); // uncompressed 24-bit thumbnail raster
				if (VERBOSE)
				{
					System.out.println("Ignoring thumbnail " + thumbnailSize + " bytes");
				}
				break;
			case "JFXX":
				int extensionCode = mBitStream.readByte();
				switch (extensionCode)
				{
					case 0x10: // jpeg encoded
					case 0x11: // 8-bit palette
					case 0x13: // 24-bit RGB
				}
				mBitStream.skip(segmentLength - 8);
				if (VERBOSE)
				{
					System.out.println("Ignoring thumbnail " + (segmentLength-8) + " bytes");
				}
				break;
			default:
				throw new IOException("Unsupported APP0 extension: " + segmentType);
		}
	}


	private JPEGImage readRaster() throws IOException
	{
		IDCTFloat idct = new IDCTFloat();
		int[] maxSampling = mFrameSegment.getMaxSampling();
		int[] dctCoefficients = mDCTCoefficients;
		int numHorMCU = (int)Math.ceil(mFrameSegment.getWidth() / (8.0 * maxSampling[0]));
		int numVerMCU = (int)Math.ceil(mFrameSegment.getHeight() / (8.0 * maxSampling[1]));
		int numComponents = mFrameSegment.getComponentCount();
		int restartMarkerIndex = 0;
		int mcuCounter = 0;

		JPEGImage image = new JPEGImage(mFrameSegment.getWidth(), mFrameSegment.getHeight(), maxSampling[0], maxSampling[1], mDensitiesUnits, mDensityX, mDensityY, mFrameSegment.getComponentCount());

		try
		{
			for (int y = 0, index = 0; y < numVerMCU; y++)
			{
				for (int x = 0; x < numHorMCU; x++, index++)
				{
					for (int component = 0; component < numComponents; component++)
					{
						ComponentInfo c = mFrameSegment.getComponent(component);
//						int[] quantizationTable = mQuantizationTables[c.getQuantizationTableId()].getTable();
						double[] quantizationTable = mQuantizationTables[c.getQuantizationTableId()].getTableD();
						int[] sampling = c.getSampling();

						for (int cy = 0; cy < sampling[1]; cy++)
						{
							for (int cx = 0; cx < sampling[0]; cx++)
							{
								if (!readDCTCofficients(dctCoefficients, component))
								{
									if (mcuCounter == 0)
									{
										throw new IOException("Error reading JPEG stream; Failed to decode Huffman code.");
									}
									image.setDamaged();
									return image;
								}

								idct.transform(dctCoefficients, quantizationTable);

								image.setData(cx, cy, sampling, component, dctCoefficients);
							}
						}
					}

					image.flushMCU(x, y);
					mcuCounter++;

					if (mRestartInterval > 0 && (((index + 1) % mRestartInterval) == 0))
					{
						if (index < numHorMCU * numVerMCU - 1) // Don't check restart marker when all MCUs are loaded
						{
							mBitStream.align();

							int restartMarker = mBitStream.readShort();
							if (restartMarker != 0xFFD0 + restartMarkerIndex)
							{
								throw new IOException("Error reading JPEG stream; Expected restart marker " + Integer.toHexString(0xFFD0 + restartMarkerIndex));
							}
							restartMarkerIndex = (restartMarkerIndex + 1) & 7;

							for (int i = MAX_CHANNELS; --i >= 0;)
							{
								mPreviousDCValue[i] = 0;
							}
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			image.setDamaged();
			throw e;
		}

		mBitStream.align();

		return image;
	}


	private boolean readDCTCofficients(int[] aCoefficients, int aComponent) throws IOException
	{
		Arrays.fill(aCoefficients, 0);

		DHTMarkerSegment dcTable = mHuffmanTables[mScanSegment.getHuffmanTableDC(aComponent)][0];
		DHTMarkerSegment acTable = mHuffmanTables[mScanSegment.getHuffmanTableAC(aComponent)][1];

		int value = dcTable.decodeSymbol(mBitStream);

		if (value == -1)
		{
			return false;
		}

		if (value > 0)
		{
			mPreviousDCValue[aComponent] += dcTable.readCoefficient(mBitStream, value);
		}

		aCoefficients[0] = mPreviousDCValue[aComponent];

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
				aCoefficients[ZIGZAG[offset]] = acTable.readCoefficient(mBitStream, codeLength);
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
}