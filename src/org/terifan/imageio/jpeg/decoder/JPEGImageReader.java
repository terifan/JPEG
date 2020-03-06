package org.terifan.imageio.jpeg.decoder;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
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
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP14Segment;
import org.terifan.imageio.jpeg.APP2Segment;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGConstants;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.JPEGImage;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.exif.Exif;


public class JPEGImageReader
{
	private BitInputStream mBitStream;
	private SOSSegment mSOSSegment;
	private Class<? extends IDCT> mIDCT;
	private boolean mStop;
	private JPEGImage mImage;
	private Decoder mDecoder;
	private int mProgressiveLevel;
	private JPEG mJPEG = new JPEG();
	private boolean mUpdateImage;
	private Exif mExif;
	private String mCompressionMode;


	public JPEGImageReader(InputStream aInputStream) throws IOException
	{
		mBitStream = new BitInputStream(aInputStream);
		mUpdateImage = true;
		mIDCT = IDCTIntegerSlow.class;
	}


	public Exif getExif()
	{
		return mExif;
	}


	public JPEGImageReader setIDCT(Class<? extends IDCT> aIDCT)
	{
		mIDCT = aIDCT;
		return this;
	}


	public JPEG decode() throws IOException
	{
		mUpdateImage = false;
		read();
		return mJPEG;
	}


	public static BufferedImage read(File aFile) throws IOException
	{
		try (InputStream input = new BufferedInputStream(new FileInputStream(aFile)))
		{
			return new JPEGImageReader(input).read();
		}
	}


	public static BufferedImage read(URL aInputStream) throws IOException
	{
		try (InputStream input = new BufferedInputStream(aInputStream.openStream()))
		{
			return new JPEGImageReader(input).read();
		}
	}


	public static BufferedImage read(InputStream aInputStream) throws IOException
	{
		return new JPEGImageReader(aInputStream).read();
	}


	public BufferedImage read() throws IOException
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

					if ((nextSegment & 0xFF00) != 0xFF00 || nextSegment == -1)
					{
						updateImage();

						System.out.println("Expected JPEG marker at " + mBitStream.getStreamOffset() + " (" + Integer.toHexString(mBitStream.getStreamOffset()) + ")");

						hexdump();

						break;
//						throw new IOException("Error in JPEG stream; expected segment marker but found: " + Integer.toString(nextSegment, 16));
					}
				}

				if (VERBOSE)
				{
					System.out.println(Integer.toString(nextSegment, 16) + " -- " + mBitStream.getStreamOffset() + " (" + Integer.toHexString(mBitStream.getStreamOffset()) + ") -- " + getSOFDescription(nextSegment));
				}

				switch (nextSegment)
				{
					case APP0:
						new APP0Segment(mJPEG).read(mBitStream);
						break;
					case APP1:
						mExif = read(mBitStream);
						break;
					case APP2:
						new APP2Segment(mJPEG).read(mBitStream);
						break;
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
						new DHTSegment(mJPEG).read(mBitStream);
						break;
					case DAC: // Arithmetic Table
						new DACSegment(mJPEG).read(mBitStream);
						break;
					case SOF0:
						mCompressionMode = "Baseline";
						mDecoder = new HuffmanDecoder(mBitStream);
						mJPEG.mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF1:
						mCompressionMode = "Extended sequential, Huffman";
						throw new IOException("Image encoding not supported: Extended sequential, Huffman");
					case SOF2:
						mCompressionMode = "Progressive, Huffman";
						mDecoder = new HuffmanDecoder(mBitStream);
						mJPEG.mProgressive = true;
						mJPEG.mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF9:
						mCompressionMode = "Extended sequential, arithmetic";
						mDecoder = new ArithmeticDecoder(mBitStream);
						mJPEG.mArithmetic = true;
						mJPEG.mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF10:
						mCompressionMode = "Progressive, arithmetic";
						mDecoder = new ArithmeticDecoder(mBitStream);
						mJPEG.mArithmetic = true;
						mJPEG.mProgressive = true;
						mJPEG.mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
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
		finally
		{
			mBitStream.close();
			mBitStream = null;
		}

		if (mImage == null)
		{
			return null;
		}

		return colorTransform();
	}


	private Exif read(BitInputStream aBitStream) throws IOException
	{
		int len = aBitStream.readInt16() - 2;

		String header = "";

		while (len-- > 0)
		{
			int c = aBitStream.readInt8();
			if (c == 0)
			{
				break;
			}
			header += (char)c;
		}

		if (header.equals("Exif"))
		{
			if (aBitStream.readInt8() != 0)
			{
				throw new IOException("Bad TIFF header data");
			}

			byte[] exif = new byte[len - 1];

			for (int i = 0; i < exif.length; i++)
			{
				exif[i] = (byte)aBitStream.readInt8();
			}

			return new Exif().decode(exif);
		}

		aBitStream.skipBytes(len);
		if (JPEGConstants.VERBOSE)
		{
			System.out.printf("  Unsupported APP1 segment content (%s)%n", header);
		}

		return null;
	}


	private BufferedImage colorTransform()
	{
		BufferedImage image = mImage.getImage();

		if (mJPEG.mICCProfile != null)
		{
			int profileClass = mJPEG.mICCProfile.getProfileClass();

			if ((profileClass != ICC_Profile.CLASS_INPUT) && (profileClass != ICC_Profile.CLASS_DISPLAY) && (profileClass != ICC_Profile.CLASS_OUTPUT) && (profileClass != ICC_Profile.CLASS_COLORSPACECONVERSION) && (profileClass != ICC_Profile.CLASS_NAMEDCOLOR) && (profileClass != ICC_Profile.CLASS_ABSTRACT))
			{
				reportError("Failed to perform color transform: Invalid profile type");
				return image;
			}

//			sun.java2d.cmm.PCMM module = sun.java2d.cmm.CMSManager.getModule();
//
//			sun.java2d.cmm.ColorTransform[] transformList = {
//				module.createTransform(mJPEG.mICCProfile, sun.java2d.cmm.ColorTransform.Any, sun.java2d.cmm.ColorTransform.In),
//				module.createTransform(ICC_Profile.getInstance(ICC_ColorSpace.CS_sRGB), sun.java2d.cmm.ColorTransform.Any, sun.java2d.cmm.ColorTransform.Out)
//			};
//
//			module.createTransform(transformList).colorConvert(image, image);

			ColorSpace colorSpace = new ICC_ColorSpace(mJPEG.mICCProfile);

			float[] colorvalue = new float[3];

			for (int y = 0; y < image.getHeight(); y++)
			{
				for (int x = 0; x < image.getWidth(); x++)
				{
					int rgb = image.getRGB(x, y);
					colorvalue[0] = (0xff & (rgb >> 16)) / 255f;
					colorvalue[1] = (0xff & (rgb >> 8)) / 255f;
					colorvalue[2] = (0xff & (rgb >> 0)) / 255f;

					colorvalue = colorSpace.toRGB(colorvalue);

					int r = (int)(255f * colorvalue[0] + 0.5f);
					int g = (int)(255f * colorvalue[1] + 0.5f);
					int b = (int)(255f * colorvalue[2] + 0.5f);
					image.setRGB(x, y, (r << 16) + (g << 8) + b);
				}
			}
		}

		return image;
	}


	private void readRaster() throws IOException
	{
		int maxSamplingX = mJPEG.mSOFSegment.getMaxHorSampling();
		int maxSamplingY = mJPEG.mSOFSegment.getMaxVerSampling();
		int numHorMCU = mJPEG.mSOFSegment.getHorMCU();
		int numVerMCU = mJPEG.mSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		for (int scanComponentIndex = 0, first = 0; scanComponentIndex < mJPEG.num_components; scanComponentIndex++)
		{
			ComponentInfo comp = mJPEG.mSOFSegment.getComponent(scanComponentIndex);
			comp.setComponentBlockOffset(first);
			first += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		mSOSSegment.prepareMCU();

		if (mImage == null)
		{
			mJPEG.mCoefficients = new int[numVerMCU][numHorMCU][mJPEG.mSOFSegment.getMaxBlocksInMCU()][64];

			mDecoder.initialize(mJPEG);

			mImage = new JPEGImage(mJPEG.mSOFSegment.getWidth(), mJPEG.mSOFSegment.getHeight(), maxSamplingX, maxSamplingY, mJPEG.num_components);
		}

		Arrays.fill(mJPEG.entropy.last_dc_val, 0);

		if (VERBOSE)
		{
			System.out.println("  " + mDecoder.getDecoderInfo(mJPEG));
		}

		mDecoder.startPass(mJPEG);

		try
		{
			int[][] mcu = new int[mJPEG.blocks_in_MCU][64];

			if (mJPEG.comps_in_scan == 1)
			{
				ComponentInfo comp = mJPEG.cur_comp_info[0];

				for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
				{
					for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
					{
						if (mcuY < numVerMCU - 1 || mcuY * mcuHeight + blockY * 8 < mJPEG.height)
						{
							for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
							{
								for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
								{
									if (mcuX < numHorMCU - 1 || mcuX * mcuWidth + blockX * 8 < mJPEG.width)
									{
										mcu[0] = mJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

										mDecoder.decodeMCU(mJPEG, mcu);
									}
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
						for (int ci = 0, blockIndex = 0; ci < mJPEG.comps_in_scan; ci++)
						{
							ComponentInfo comp = mJPEG.cur_comp_info[ci];

							for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
							{
								for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++, blockIndex++)
								{
									mcu[blockIndex] = mJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];
								}
							}
						}

						mDecoder.decodeMCU(mJPEG, mcu);

						for (int ci = 0, blockIndex = 0; ci < mJPEG.comps_in_scan; ci++)
						{
							ComponentInfo comp = mJPEG.cur_comp_info[ci];

							for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
							{
								for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++, blockIndex++)
								{
									mJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX] = mcu[blockIndex];
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

		mProgressiveLevel++;
		mBitStream.align();

//		updateImage();
//		ImageIO.write(mImage.getImage(), "png", new File("d:\\output-"+mProgressiveLevel+".png"));

		if (VERBOSE)
		{
			System.out.println("======== " + mBitStream.getStreamOffset() + "(" + Integer.toHexString(mBitStream.getStreamOffset()) + ") / " + mProgressiveLevel + " ========================================================================================================================================================================");
		}
	}


	private void updateImage()
	{
		int maxSamplingX = mJPEG.mSOFSegment.getMaxHorSampling();
		int maxSamplingY = mJPEG.mSOFSegment.getMaxVerSampling();
		int numHorMCU = mJPEG.mSOFSegment.getHorMCU();
		int numVerMCU = mJPEG.mSOFSegment.getVerMCU();

		IDCT idct;
		try
		{
			idct = mIDCT.newInstance();
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}

		int mcuW = 8 * maxSamplingX;
		int mcuH = 8 * maxSamplingY;

		int[][][][] coefficients = mJPEG.mCoefficients;

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ObjectOutputStream oos = new ObjectOutputStream(baos))
			{
				oos.writeObject(coefficients);
			}
			try (ObjectInputStream oos = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())))
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

//		try (DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(new FileOutputStream("d:\\jpeg.data"))))
//		{
//			for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
//			{
//				for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
//				{
//					for (int blockIndex = 0; blockIndex < blockLookup.length; blockIndex++)
//					{
//						for (int i = 0; i < coefficients[mcuY][mcuX][blockIndex].length; i++)
//						{
//							dos.writeShort(coefficients[mcuY][mcuX][blockIndex][i]);
//						}
//					}
//				}
//			}
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.err);
//		}

		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
				for (int blockIndex = 0; blockIndex < blockLookup.length; blockIndex++)
				{
					ComponentInfo comp = mJPEG.mSOFSegment.getComponent(blockLookup[blockIndex]);

					QuantizationTable quantizationTable = mJPEG.mQuantizationTables[comp.getQuantizationTableId()];

					idct.transform(coefficients[mcuY][mcuX][blockIndex], quantizationTable);
				}
			}
		}

		int c0 = mJPEG.mSOFSegment.getComponent(0).getComponentBlockOffset();
		int c1 = mJPEG.components.length == 1 ? 0 : mJPEG.mSOFSegment.getComponent(1).getComponentBlockOffset();
		int c2 = mJPEG.components.length == 1 ? 0 : mJPEG.mSOFSegment.getComponent(2).getComponentBlockOffset();

		int h0 = mJPEG.components[0].getHorSampleFactor();
		int v0 = mJPEG.components[0].getVerSampleFactor();
		int h1 = mJPEG.components.length == 1 ? 0 : mJPEG.components[1].getHorSampleFactor();
		int v1 = mJPEG.components.length == 1 ? 0 : mJPEG.components[1].getVerSampleFactor();
		int h2 = mJPEG.components.length == 1 ? 0 : mJPEG.components[2].getHorSampleFactor();
		int v2 = mJPEG.components.length == 1 ? 0 : mJPEG.components[2].getVerSampleFactor();

		if (mJPEG.components.length == 1 && (h0 != 1 || v0 != 1))
		{
			throw new IllegalStateException("Unsupported subsampling");
		}

		for (int iy = 0; iy < mJPEG.height; iy++)
		{
			for (int ix = 0; ix < mJPEG.width; ix++)
			{
//		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
//		{
//			for (int blockY = 0; blockY < maxSamplingY; blockY++)
//			{
//				for (int y = 0; y < 8; y++)
//				{
//					for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
//					{
//						for (int blockX = 0; blockX < maxSamplingX; blockX++)
//						{
//							for (int x = 0; x < 8; x++)
//							{
//								int ix = mcuX * mcuW + blockX * 8 + x;
//								int iy = mcuY * mcuH + blockY * 8 + y;
//
//								if (ix < mJPEG.width && iy < mJPEG.height)
//								{
									int ixh0 = (ix % mcuW) * h0 / maxSamplingX;
									int iyv0 = (iy % mcuH) * v0 / maxSamplingY;

									int lu = coefficients[iy / mcuH][ix / mcuW][c0 + (ixh0 / 8) + h0 * (iyv0 / 8)][(ixh0 % 8) + 8 * (iyv0 % 8)];
									int color;

									if (mJPEG.num_components == 1)
									{
										color = mJPEG.mColorSpace.yccToRgb(lu, 0, 0);
									}
									else if (mJPEG.num_components == 3)
									{
										int cb;
										int cr;

										if (h0 == 2 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
										{
											cb = upsampleChroma(ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_3X3);
											cr = upsampleChroma(ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_3X3);
										}
										else if (h0 == 1 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
										{
											cb = upsampleChroma(ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_1X3);
											cr = upsampleChroma(ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_1X3);
										}
										else if (h0 == 2 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
										{
											cb = upsampleChroma(ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_3X1);
											cr = upsampleChroma(ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_3X1);
										}
										else
										{
											cb = upsampleChroma(ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_1X1);
											cr = upsampleChroma(ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_1X1);
										}

										color = mJPEG.mColorSpace.yccToRgb(lu, cb, cr);
									}
									else
									{
										throw new IllegalStateException();
									}

									mImage.getImage().setRGB(ix, iy, color);
//								}
//							}
//						}
//					}
//				}
			}
		}
	}

	private final static int[][] KERNEL_1X1 =
	{
		{1}
	};

	private final static int[][] KERNEL_3X3 =
	{
		{1,2,1},
		{2,4,2},
		{1,2,1}
	};

	private final static int[][] KERNEL_1X3 =
	{
		{1},
		{2},
		{1}
	};

	private final static int[][] KERNEL_3X1 =
	{
		{1,2,1}
	};


	private int upsampleChroma(int ix, int iy, int aMcuW, int aMcuH, int aSamplingHor, int aSamplingVer, int aMaxSamplingX, int aMaxSamplingY, int[][][][] aCoefficients, int aComponentOffset, int[][] kernel)
	{
		int sum = 0;
		int total = 0;

		int x = ix - kernel[0].length / 2;
		int y = iy - kernel.length / 2;
		int iw = mJPEG.width;
		int ih = mJPEG.height;

		for (int fy = 0; fy < kernel.length; fy++)
		{
			for (int fx = 0; fx < kernel[0].length; fx++)
			{
				if (x + fx >= 0 && y + fy >= 0 && x + fx < iw && y + fy < ih)
				{
					int bx = ((x + fx) % aMcuW) * aSamplingHor / aMaxSamplingX;
					int by = ((y + fy) % aMcuH) * aSamplingVer / aMaxSamplingY;

					int w = kernel[fy][fx];

					sum += w * aCoefficients[(y + fy) / aMcuH][(x + fx) / aMcuW][aComponentOffset + (bx / 8) + aSamplingHor * (by / 8)][(bx % 8) + 8 * (by % 8)];

					total += w;
				}
			}
		}

//		return (sum+kernel[0].length*kernel.length-1) / total;
		return (int)Math.round(sum / (double)total);
	}


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

				if (b0 == -1)
				{
					return;
				}

				System.out.printf("%02x ", b0);

				if (b1 == 255 && b0 != 0)
				{
					System.out.println();
					System.out.println("=> " + streamOffset + " +" + cnt + " (" + Integer.toHexString(streamOffset) + ")");
					return;
				}

				b1 = b0;
				if ((c % 8) == 7)
				{
					System.out.print(" ");
				}
			}
			System.out.println();
		}
	}


	// TODO
	protected void reportError(String aText)
	{
	}


	public String getSubSampling()
	{
		StringBuilder sb = new StringBuilder();
		for (ComponentInfo ci : mJPEG.components)
		{
			if (sb.length() > 0)
			{
				sb.append(",");
			}
			sb.append(ci.getHorSampleFactor()+"x"+ci.getVerSampleFactor());
		}

		switch (sb.toString())
		{
			case "1x1,1x1,1x1":	return "4:4:4"; // 1 1
			case "1x2,1x1,1x1":	return "4:4:0"; // 1 2
			case "2----------":	return "4:4:1"; // 1 4
			case "2x1,1x1,1x1":	return "4:2:2"; // 2 1
			case "2x2,1x1,1x1":	return "4:2:0"; // 2 2
			case "3----------":	return "4:2:1"; // 2 4
			case "4x1,1x1,1x1":	return "4:1:1"; // 4 1
			case "4x1,2x1,2x1":	return "4:1:0"; // 4 2
		}

		return sb.toString();
	}


	public String getCompressionMode()
	{
		return mCompressionMode;
	}
}
