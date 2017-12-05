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
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.encoder.FDCTInteger;


public class JPEGImageReader extends JPEGConstants
{
	final static int MAX_CHANNELS = 3;

	private BitInputStream mBitStream;
	private final DHTMarkerSegment[][] mHuffmanTables;
	private final DQTMarkerSegment[] mQuantizationTables;
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

		mBitStream = new BitInputStream(aInputStream);
	}


	public static BufferedImage read(InputStream aInputStream) throws IOException
	{
		return new JPEGImageReader(aInputStream).readImpl();
	}


	private BufferedImage readImpl() throws IOException
	{
		JPEGImage image = null;

		try
		{
			int nextSegment = mBitStream.readInt16();

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
				nextSegment = mBitStream.readInt16();

				while ((nextSegment & 0xFF00) == 0)
				{
					nextSegment = ((0xFF & nextSegment) << 8) | mBitStream.readInt8();
				}

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
					case SOF0:
						mFrameSegment = new SOFMarkerSegment(mBitStream);
						break;
					case SOS:
					{
						mScanSegment = new SOSMarkerSegment(mBitStream);
						image = readRaster();
						if (image.isDamaged())
						{
							return image.getImage();
						}
						break;
					}
					case DRI:
						mBitStream.skipBytes(2); // skip length
						mRestartInterval = mBitStream.readInt16();
						break;
					case COM:
						mBitStream.skipBytes(mBitStream.readInt16() - 2);
						break;
					case EOI:
						break;
					case SOF2:
						throw new IOException("Progressive images not supported.");
					default:
						mBitStream.skipBytes(mBitStream.readInt16() - 2);
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

		return image == null ? null : image.getImage();
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


	private JPEGImage readRaster() throws IOException
	{
//		IDCT idct = new IDCTFloat();
		IDCT idct = new IDCTIntegerSlow();
//		IDCT idct = new IDCTIntegerFast();
//		IDCT idct = new IDCTInteger2();
		int maxSamplingX = mFrameSegment.getMaxSamplingX();
		int maxSamplingY = mFrameSegment.getMaxSamplingY();
		int numHorMCU = (int)Math.ceil(mFrameSegment.getWidth() / (8.0 * maxSamplingX));
		int numVerMCU = (int)Math.ceil(mFrameSegment.getHeight() / (8.0 * maxSamplingY));
		int numComponents = mFrameSegment.getComponentCount();
		int restartMarkerIndex = 0;
		int mcuCounter = 0;

		int[][][][][] dctCoefficients = new int[numHorMCU][maxSamplingY][maxSamplingX][3][64];

		JPEGImage image = new JPEGImage(mFrameSegment.getWidth(), mFrameSegment.getHeight(), maxSamplingX, maxSamplingY, mDensitiesUnits, mDensityX, mDensityY, mFrameSegment.getComponentCount());

		try
		{
			for (int y = 0, index = 0; y < numVerMCU; y++)
			{
				for (int x = 0; x < numHorMCU; x++)
				{
					for (int component = 0; component < numComponents; component++)
					{
						ComponentInfo comp = mFrameSegment.getComponent(component);
						int samplingX = comp.getSamplingX();
						int samplingY = comp.getSamplingY();

						for (int cy = 0; cy < samplingY; cy++)
						{
							for (int cx = 0; cx < samplingX; cx++)
							{
								if (!readDCTCofficients(dctCoefficients[x][cy][cx][component], component))
								{
									if (mcuCounter == 0)
									{
										throw new IOException("Error reading JPEG stream; Failed to decode Huffman code.");
									}
									image.setDamaged();
									return image;
								}
							}
						}
					}
				}

				int[][][] buffers = new int[numHorMCU][3][maxSamplingX * maxSamplingY * 8 * 8];

				for (int x = 0; x < numHorMCU; x++)
				{
					for (int component = 0; component < numComponents; component++)
					{
						ComponentInfo comp = mFrameSegment.getComponent(component);
						DQTMarkerSegment quantizationTable = mQuantizationTables[comp.getQuantizationTableId()];
						int samplingX = comp.getSamplingX();
						int samplingY = comp.getSamplingY();

						for (int cy = 0; cy < samplingY; cy++)
						{
							for (int cx = 0; cx < samplingX; cx++)
							{
								idct.transform(dctCoefficients[x][cy][cx][component], quantizationTable);
							}
						}
					}
				}

				for (int x = 0; x < numHorMCU; x++)
				{
					for (int component = 0; component < numComponents; component++)
					{
						ComponentInfo comp = mFrameSegment.getComponent(component);
						int samplingX = comp.getSamplingX();
						int samplingY = comp.getSamplingY();

						for (int cy = 0; cy < samplingY; cy++)
						{
							for (int cx = 0; cx < samplingX; cx++)
							{
								int a, b;
								if (cx == samplingX - 1 && x == numHorMCU - 1)
								{
									a = numHorMCU - 1;
									b = samplingX - 1;
								}
								else if (cx == samplingX - 1)
								{
									a = x + 1;
									b = 0;
								}
								else
								{
									a = x;
									b = cx + 1;
								}

								image.setData(cx, cy, samplingX, samplingY, buffers[x][component], dctCoefficients[x][cy][cx][component], dctCoefficients[a][cy][b][component]);
							}
						}
					}
				}

				for (int x = 0; x < numHorMCU; x++, index++)
				{
					image.flushMCU(x, y, buffers[x]);
					mcuCounter++;

					if (mRestartInterval > 0 && (((index + 1) % mRestartInterval) == 0))
					{
						if (index < numHorMCU * numVerMCU - 1) // Don't check restart marker when all MCUs are loaded
						{
							mBitStream.align();

							int restartMarker = mBitStream.readInt16();
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
}