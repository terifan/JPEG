package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.SOSSegment;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DQTSegment;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Arrays;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP14Segment;
import org.terifan.imageio.jpeg.ColorSpace;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.JPEGImage;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.exif.JPEGExif;


public class JPEGImageReader
{
	final static int MAX_CHANNELS = 3;

	private BitInputStream mBitStream;
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


	public static BufferedImage read(File aFile) throws IOException
	{
		try (InputStream input = new BufferedInputStream(new FileInputStream(aFile)))
		{
			return new JPEGImageReader(input, IDCTIntegerFast.class).readImpl();
		}
	}


	public static BufferedImage read(URL aInputStream) throws IOException
	{
		try (InputStream input = new BufferedInputStream(aInputStream.openStream()))
		{
			return new JPEGImageReader(input, IDCTIntegerFast.class).readImpl();
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
				if (mBitStream.getUnreadMarker() != 0)
				{
					nextSegment = 0xff00 | mBitStream.getUnreadMarker();
					mBitStream.setUnreadMarker(0);
				}
				else
				{
					nextSegment = mBitStream.readInt16();

//					while ((nextSegment & 0xFF00) == 0)
//					{
//						nextSegment = ((0xFF & nextSegment) << 8) | mBitStream.readInt8();
//					}

					if ((nextSegment & 0xFF00) != 0xFF00)
					{
						updateImage();
						
						System.out.println("Expected JPEG marker at " + mBitStream.getStreamOffset() + " ("+Integer.toHexString(mBitStream.getStreamOffset())+")");

						hexdump();

						break;
	//					throw new IOException("Error in JPEG stream; expected segment marker but found: " + Integer.toString(nextSegment, 16));
					}
				}

				if (VERBOSE)
				{
					System.out.println(Integer.toString(nextSegment, 16) + " -- " + mBitStream.getStreamOffset() + " ("+Integer.toHexString(mBitStream.getStreamOffset())+") -- " + getSOFDescription(nextSegment));
				}

				switch (nextSegment)
				{
					case APP0:
						new APP0Segment(mJPEG).read(mBitStream);
						break;
					case APP1:
						try
						{
							new JPEGExif().read(mBitStream);
						}
						catch (Throwable e)
						{
							System.out.println("Error reading metadata");
							e.printStackTrace(System.err);
						}
						break;
					case APP2:
					case APP12:
					case APP13:
						// TODO
						mBitStream.skipBytes(mBitStream.readInt16() - 2); // skip length
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
						mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF1: // Extended sequential, Huffman
						throw new IOException("Image encoding not supported: Extended sequential, Huffman");
					case SOF2: // Progressive, Huffman
						mDecoder = new HuffmanDecoder(mBitStream);
						mJPEG.mProgressive = true;
						mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF9: // Extended sequential, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
						mJPEG.mArithmetic = true;
						mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF10: // Progressive, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
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
						mSOSSegment = new SOSSegment(mJPEG).read(mBitStream);
						mBitStream.setHandleMarkers(true);
						readRaster();
						mBitStream.setHandleMarkers(false);
						break;
					case DRI:
						mBitStream.skipBytes(2); // skip length
						mJPEG.restart_interval = mBitStream.readInt16();
						break;
					case COM:
						mBitStream.skipBytes(mBitStream.readInt16() - 2);
						break;
					case EOI:
						if (mUpdateImage)
						{
							updateImage();
						}
						mStop = true;
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
			e.printStackTrace(System.out);
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
		int maxSamplingX = mSOFSegment.getMaxHorSampling();
		int maxSamplingY = mSOFSegment.getMaxVerSampling();
		int numHorMCU = mSOFSegment.getHorMCU();
		int numVerMCU = mSOFSegment.getVerMCU();

		for (int scanComponentIndex = 0, first = 0; scanComponentIndex < mJPEG.num_components; scanComponentIndex++)
		{
			ComponentInfo comp = mSOFSegment.getComponent(scanComponentIndex);
			comp.setComponentBlockOffset(first);
			first += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		prepareMCU(mJPEG, mSOFSegment, mSOSSegment);

		if (mImage == null)
		{
			mJPEG.mCoefficients = new int[numVerMCU][numHorMCU][mJPEG.blocks_in_MCU][64];

			mDecoder.initialize(mJPEG);

			mImage = new JPEGImage(mSOFSegment.getWidth(), mSOFSegment.getHeight(), maxSamplingX, maxSamplingY, mJPEG.num_components);
		}

		mDecoder.startPass(mJPEG);

		try
		{
			int[][] mcu = new int[mJPEG.blocks_in_MCU][64];

			if (mJPEG.comps_in_scan == 1)
			{
				ComponentInfo comp = mJPEG.cur_comp_info[0];
				int componentBlockOffset = comp.getComponentBlockOffset();

				for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
				{
					for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
					{
						for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
						{
							for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
							{
//								System.out.println(mProgressiveLevel+" "+mcuY+" "+mcuX+" "+blockY+" "+blockX+" "+mBitStream.getStreamOffset());

								mcu[0] = mJPEG.mCoefficients[mcuY][mcuX][componentBlockOffset + comp.getHorSampleFactor() * blockY + blockX];

								mDecoder.decodeMCU(mJPEG, mcu);
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
//						System.out.println(mProgressiveLevel+" "+mcuY+" "+mcuX);

//						for (int blockIndex = 0; blockIndex < mJPEG.blocks_in_MCU; blockIndex++)
//							mcu[blockIndex] = mJPEG.mCoefficients[mcuY][mcuX][blockIndex];

						mDecoder.decodeMCU(mJPEG, mcu);

						for (int blockIndex = 0; blockIndex < mJPEG.blocks_in_MCU; blockIndex++)
						{
							for (int i = 0; i < 64; i++)
							{
								mJPEG.mCoefficients[mcuY][mcuX][blockIndex][i] += mcu[blockIndex][i];
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

		mProgressiveLevel++;
		mBitStream.align();

		if (VERBOSE) System.out.println("======== " + mBitStream.getStreamOffset() + "("+Integer.toHexString(mBitStream.getStreamOffset())+") / " + mProgressiveLevel + " ========================================================================================================================================================================");
	}


	public static void prepareMCU(JPEG aJPEG, SOFSegment aSOFSegment, SOSSegment aSOSSegment)
	{
		aJPEG.blocks_in_MCU = 0;

		for (int scanComponentIndex = 0; scanComponentIndex < aJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = aSOFSegment.getComponentByScan(aSOSSegment.getComponent(scanComponentIndex));
			aJPEG.blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		aJPEG.MCU_membership = new int[aJPEG.blocks_in_MCU];
		aJPEG.cur_comp_info = new ComponentInfo[aJPEG.comps_in_scan];

		for (int scanComponentIndex = 0, blockIndex = 0; scanComponentIndex < aJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = aSOFSegment.getComponentByScan(aSOSSegment.getComponent(scanComponentIndex));
			comp.setTableAC(aSOSSegment.getACTable(scanComponentIndex));
			comp.setTableDC(aSOSSegment.getDCTable(scanComponentIndex));

			aJPEG.cur_comp_info[scanComponentIndex] = comp;

			for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, blockIndex++)
			{
				aJPEG.MCU_membership[blockIndex] = scanComponentIndex;
			}

			if (VERBOSE) System.out.println(comp);
		}
	}


	private void updateImage()
	{
		int maxSamplingX = mSOFSegment.getMaxHorSampling();
		int maxSamplingY = mSOFSegment.getMaxVerSampling();
		int numHorMCU = mSOFSegment.getHorMCU();
		int numVerMCU = mSOFSegment.getVerMCU();

		IDCT idct;
		try
		{
			idct = mIDCT.newInstance();
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}

		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		int[][][][] coefficients = mJPEG.mCoefficients;

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ObjectOutputStream oos = new ObjectOutputStream(baos))
			{
				oos.writeObject(coefficients);
			}
			try (ObjectInputStream oos = new ObjectInputStream (new ByteArrayInputStream(baos.toByteArray())))
			{
				coefficients = (int[][][][])oos.readObject();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}


		int[] blockLookup = new int[12];

		int cp = 0;
		int cii = 0;
		for (ComponentInfo ci : mJPEG.components)
		{
			for (int i = 0; i < ci.getVerSampleFactor() * ci.getHorSampleFactor(); i++, cp++)
			{
				blockLookup[cp] = cii;
			}
			cii++;
		}
		blockLookup = Arrays.copyOfRange(blockLookup, 0, cp);

		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
				for (int blockIndex = 0; blockIndex < blockLookup.length; blockIndex++)
				{
					ComponentInfo comp = mSOFSegment.getComponent(blockLookup[blockIndex]);

					QuantizationTable quantizationTable = mJPEG.mQuantizationTables[comp.getQuantizationTableId()];

					idct.transform(coefficients[mcuY][mcuX][blockIndex], quantizationTable);
				}

				for (int blockY = 0; blockY < maxSamplingY; blockY++)
				{
					for (int blockX = 0; blockX < maxSamplingX; blockX++)
					{
						for (int y = 0; y < 8; y++)
						{
							for (int x = 0; x < 8; x++)
							{
								int lu;
								int cb;
								int cr;
								if (mJPEG.components[0].getHorSampleFactor() == 1 && mJPEG.components[0].getVerSampleFactor() == 1 && mJPEG.components[1].getHorSampleFactor() == 1 && mJPEG.components[1].getVerSampleFactor() == 1 && mJPEG.components[2].getHorSampleFactor() == 1 && mJPEG.components[2].getVerSampleFactor() == 1)
								{
									lu = coefficients[mcuY][mcuX][0][y * 8 + x];
									cb = coefficients[mcuY][mcuX][1][y * 8 + x];
									cr = coefficients[mcuY][mcuX][2][y * 8 + x];
								}
								else if (mJPEG.components[0].getHorSampleFactor() == 2 && mJPEG.components[0].getVerSampleFactor() == 2 && mJPEG.components[1].getHorSampleFactor() == 1 && mJPEG.components[1].getVerSampleFactor() == 1 && mJPEG.components[2].getHorSampleFactor() == 1 && mJPEG.components[2].getVerSampleFactor() == 1)
								{
									lu = coefficients[mcuY][mcuX][blockY * 2 + blockX][y * 8 + x];
									cb = coefficients[mcuY][mcuX][4][8 * blockY * 4 + y / 2 * 8 + x / 2 + 4 * blockX];
									cr = coefficients[mcuY][mcuX][5][8 * blockY * 4 + y / 2 * 8 + x / 2 + 4 * blockX];
								}
								else if (mJPEG.components[0].getHorSampleFactor() == 2 && mJPEG.components[0].getVerSampleFactor() == 1 && mJPEG.components[1].getHorSampleFactor() == 1 && mJPEG.components[1].getVerSampleFactor() == 1 && mJPEG.components[2].getHorSampleFactor() == 1 && mJPEG.components[2].getVerSampleFactor() == 1)
								{
									lu = coefficients[mcuY][mcuX][blockX][y * 8 + x];
									cb = coefficients[mcuY][mcuX][2][8 * blockY * 4 + y / 2 * 8 + x / 2 + 4 * blockX];
									cr = coefficients[mcuY][mcuX][3][8 * blockY * 4 + y / 2 * 8 + x / 2 + 4 * blockX];
								}
								else
								{
									throw new IllegalStateException("Unsupported subsampling");
								}

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


//	public void debugprint(int aMcuX, int aMcuY)
//	{
//		printTables(new int[][]{mDctCoefficients[aMcuY][aMcuX][0][0][0], mDctCoefficients[aMcuY][aMcuX][0][1][0], mDctCoefficients[aMcuY][aMcuX][1][0][0], mDctCoefficients[aMcuY][aMcuX][1][1][0], mDctCoefficients[aMcuY][aMcuX][0][0][1], mDctCoefficients[aMcuY][aMcuX][0][0][2]});
//		System.out.println();
//	}


	public void hexdump() throws IOException
	{
		int streamOffset = mBitStream.getStreamOffset();

		int cnt = 0;
		int b1 = 0;
		for (int r = 0; r < 1000; r++)
		{
			for (int c = 0; c < 96; c++, cnt++)
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
