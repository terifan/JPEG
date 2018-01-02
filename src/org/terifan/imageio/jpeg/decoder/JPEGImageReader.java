package org.terifan.imageio.jpeg.decoder;

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
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP14Segment;
import org.terifan.imageio.jpeg.APP2Segment;
import org.terifan.imageio.jpeg.ColorSpace;
import org.terifan.imageio.jpeg.ColorSpaceType;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.JPEGImage;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.exif.JPEGExif;
import sun.java2d.cmm.CMSManager;
import sun.java2d.cmm.ColorTransform;
import sun.java2d.cmm.PCMM;


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


	public JPEGImageReader(InputStream aInputStream) throws IOException
	{
		mBitStream = new BitInputStream(aInputStream);
		mUpdateImage = true;
		mIDCT = IDCTIntegerSlow.class;
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

					if ((nextSegment & 0xFF00) != 0xFF00)
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
						try
						{
							new JPEGExif().read(mBitStream);
						}
						catch (Throwable e)
						{
							System.err.println("Error reading metadata");
							e.printStackTrace(System.err);
						}
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
					case SOF0: // Baseline
						mDecoder = new HuffmanDecoder(mBitStream);
						mJPEG.mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF1: // Extended sequential, Huffman
						throw new IOException("Image encoding not supported: Extended sequential, Huffman");
					case SOF2: // Progressive, Huffman
						mDecoder = new HuffmanDecoder(mBitStream);
						mJPEG.mProgressive = true;
						mJPEG.mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF9: // Extended sequential, arithmetic
						mDecoder = new ArithmeticDecoder(mBitStream);
						mJPEG.mArithmetic = true;
						mJPEG.mSOFSegment = new SOFSegment(mJPEG).read(mBitStream);
						break;
					case SOF10: // Progressive, arithmetic
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
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
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

			PCMM module = CMSManager.getModule();

			ColorTransform[] transformList = {
				module.createTransform(mJPEG.mICCProfile, ColorTransform.Any, ColorTransform.In),
				module.createTransform(ICC_Profile.getInstance(ICC_ColorSpace.CS_sRGB), ColorTransform.Any, ColorTransform.Out)
			};

			module.createTransform(transformList).colorConvert(image, image);
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

		prepareMCU(mJPEG, mJPEG.mSOFSegment, mSOSSegment);

		if (mImage == null)
		{
//    cid0 = cinfo->comp_info[0].component_id;
//    cid1 = cinfo->comp_info[1].component_id;
//    cid2 = cinfo->comp_info[2].component_id;
//
//    /* First try to guess from the component IDs */
//    if      (cid0 == 0x01 && cid1 == 0x02 && cid2 == 0x03)
//      cinfo->jpeg_color_space = JCS_YCbCr;
//    else if (cid0 == 0x01 && cid1 == 0x22 && cid2 == 0x23)
//      cinfo->jpeg_color_space = JCS_BG_YCC;
//    else if (cid0 == 0x52 && cid1 == 0x47 && cid2 == 0x42)
//      cinfo->jpeg_color_space = JCS_RGB;	/* ASCII 'R', 'G', 'B' */
//    else if (cid0 == 0x72 && cid1 == 0x67 && cid2 == 0x62)
//      cinfo->jpeg_color_space = JCS_BG_RGB;	/* ASCII 'r', 'g', 'b' */
//    else if (cinfo->saw_JFIF_marker)
//      cinfo->jpeg_color_space = JCS_YCbCr;	/* assume it's YCbCr */
//    else if (cinfo->saw_Adobe_marker) {
//      switch (cinfo->Adobe_transform) {
//      case 0:
//	cinfo->jpeg_color_space = JCS_RGB;
//	break;
//      case 1:
//	cinfo->jpeg_color_space = JCS_YCbCr;
//	break;
//      default:
//	WARNMS1(cinfo, JWRN_ADOBE_XFORM, cinfo->Adobe_transform);
//	cinfo->jpeg_color_space = JCS_YCbCr;	/* assume it's YCbCr */
//	break;
//      }

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
						mDecoder.decodeMCU(mJPEG, mcu);

						for (int ci = 0, blockIndex = 0; ci < mJPEG.comps_in_scan; ci++)
						{
							ComponentInfo comp = mJPEG.cur_comp_info[ci];

							for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
							{
								for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++, blockIndex++)
								{
									int[] tmp = mJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

									for (int i = 0; i < 64; i++)
									{
										tmp[i] += mcu[blockIndex][i];
									}
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

		if (mProgressiveLevel == 311)
		{
			updateImage();
			mStop = true;
		}

		if (VERBOSE)
		{
			System.out.println("======== " + mBitStream.getStreamOffset() + "(" + Integer.toHexString(mBitStream.getStreamOffset()) + ") / " + mProgressiveLevel + " ========================================================================================================================================================================");
		}
	}


	public static void prepareMCU(JPEG aJPEG, SOFSegment aSOFSegment, SOSSegment aSOSSegment)
	{
		aJPEG.blocks_in_MCU = 0;

		for (int scanComponentIndex = 0; scanComponentIndex < aJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = aSOFSegment.getComponentById(aSOSSegment.getComponentByIndex(scanComponentIndex));
			aJPEG.blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		aJPEG.MCU_membership = new int[aJPEG.blocks_in_MCU];
		aJPEG.cur_comp_info = new ComponentInfo[aJPEG.comps_in_scan];

		if (VERBOSE)
		{
			System.out.println("MCU");
		}

		for (int scanComponentIndex = 0, blockIndex = 0; scanComponentIndex < aJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = aSOFSegment.getComponentById(aSOSSegment.getComponentByIndex(scanComponentIndex));
			comp.setTableAC(aSOSSegment.getACTable(scanComponentIndex));
			comp.setTableDC(aSOSSegment.getDCTable(scanComponentIndex));

			aJPEG.cur_comp_info[scanComponentIndex] = comp;

			for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, blockIndex++)
			{
				aJPEG.MCU_membership[blockIndex] = scanComponentIndex;
			}

			if (VERBOSE)
			{
				System.out.println("  " + comp);
			}
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

		double[][][] w = 
		{
			{
				{0.50,0.50,0.25},
				{0.50,1.00,0.25},
				{0.25,0.25,0.25}
			},
			{
				{0.25,0.50,0.50},
				{0.25,1.00,0.50},
				{0.25,0.25,0.25}
			},
			{
				{0.25,0.25,0.25},
				{0.50,1.00,0.25},
				{0.50,0.50,0.25}
			},
			{
				{0.25,0.25,0.25},
				{0.25,1.00,0.50},
				{0.25,0.50,0.50}
			}
		};

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
		for (int iy = 0; iy < mJPEG.height; iy++)
		{
		for (int ix = 0; ix < mJPEG.width; ix++)
		{
//								int ix = mcuX * mcuW + blockX * 8 + x;
//								int iy = mcuY * mcuH + blockY * 8 + y;

								if (ix < mJPEG.width && iy < mJPEG.height)
								{
									int ixh0 = (ix % mcuW) * h0 / maxSamplingX;
									int iyv0 = (iy % mcuH) * v0 / maxSamplingY;
									int ixh1 = (ix % mcuW) * h1 / maxSamplingX;
									int iyv1 = (iy % mcuH) * v1 / maxSamplingY;
									int ixh2 = (ix % mcuW) * h2 / maxSamplingX;
									int iyv2 = (iy % mcuH) * v2 / maxSamplingY;

									if (mJPEG.components.length == 1)
									{
										int lu = coefficients[iy / mcuH][ix / mcuW][c0 + (ixh0 / 8) + h0 * (iyv0 / 8)][(ixh0 % 8) + 8 * (iyv0 % 8)];

										mImage.getRaster()[iy * mJPEG.width + ix] = (lu << 16) + (lu << 8) + lu;
									}
									else
									{
										int lu11 = coefficients[iy / mcuH][ix / mcuW][c0 + (ixh0 / 8) + h0 * (iyv0 / 8)][(ixh0 % 8) + 8 * (iyv0 % 8)];
										int cb11 = coefficients[iy / mcuH][ix / mcuW][c1 + (ixh1 / 8) + h1 * (iyv1 / 8)][(ixh1 % 8) + 8 * (iyv1 % 8)];
										int cr11 = coefficients[iy / mcuH][ix / mcuW][c2 + (ixh2 / 8) + h2 * (iyv2 / 8)][(ixh2 % 8) + 8 * (iyv2 % 8)];

										if (ix > 0 && iy > 0 && ix < mJPEG.width - 1 && iy < mJPEG.height - 1)
										{
											if (h0 != h1 || v0 != v1)
											{
												int px = ix - 1;
												int py = iy - 1;
												int nx = ix + 1;
												int ny = iy + 1;

												int pxh1 = (px % mcuW) * h1 / maxSamplingX;
												int pyv1 = (py % mcuH) * v1 / maxSamplingY;
												int nxh1 = (nx % mcuW) * h1 / maxSamplingX;
												int nyv1 = (ny % mcuH) * v1 / maxSamplingY;

												int c00 = coefficients[py / mcuH][px / mcuW][c1 + (pxh1 / 8) + h1 * (pyv1 / 8)][(pxh1 % 8) + 8 * (pyv1 % 8)];
												int c10 = coefficients[py / mcuH][ix / mcuW][c1 + (ixh1 / 8) + h1 * (pyv1 / 8)][(ixh1 % 8) + 8 * (pyv1 % 8)];
												int c20 = coefficients[py / mcuH][nx / mcuW][c1 + (nxh1 / 8) + h1 * (pyv1 / 8)][(nxh1 % 8) + 8 * (pyv1 % 8)];

												int c01 = coefficients[iy / mcuH][px / mcuW][c1 + (pxh1 / 8) + h1 * (iyv1 / 8)][(pxh1 % 8) + 8 * (iyv1 % 8)];
												int c11 = cb11;
												int c21 = coefficients[iy / mcuH][nx / mcuW][c1 + (nxh1 / 8) + h1 * (iyv1 / 8)][(nxh1 % 8) + 8 * (iyv1 % 8)];

												int c02 = coefficients[ny / mcuH][px / mcuW][c1 + (pxh1 / 8) + h1 * (nyv1 / 8)][(pxh1 % 8) + 8 * (nyv1 % 8)];
												int c12 = coefficients[ny / mcuH][ix / mcuW][c1 + (ixh1 / 8) + h1 * (nyv1 / 8)][(ixh1 % 8) + 8 * (nyv1 % 8)];
												int c22 = coefficients[ny / mcuH][nx / mcuW][c1 + (nxh1 / 8) + h1 * (nyv1 / 8)][(nxh1 % 8) + 8 * (nyv1 % 8)];

												int z = (ix & 1) + 2 * (iy & 1);
												cb11 = (int)((w[z][0][0]*c00 + w[z][0][1]*c10 + w[z][0][2]*c20 + w[z][1][0]*c01 + w[z][1][1]*c11 + w[z][1][2]*c21 + w[z][2][0]*c02 + w[z][2][1]*c12 + w[z][2][2]*c22) / (1+5*0.25+3*0.5));

//												cb11 = (int)((c11 + 0.06 * (c00+c20+c02+c22) + 0.33 * (c01+c10+c20+c02)) / (1+0.06*4+0.33*4));
//												cb11 = (int)((c11 + 0.25 * (c00+c20+c02+c22) + 0.50 * (c01+c10+c20+c02)) / (1+0.25*4+0.50*4));
//												cb11 = (c00 + c10 + c20 + c01 + c11 + c21 + c02 + c12 + c22) / 9;
											}

											if (h0 != h2 || v0 != v2)
											{
												int px = ix - 1;
												int py = iy - 1;
												int nx = ix + 1;
												int ny = iy + 1;

												int pxh2 = (px % mcuW) * h2 / maxSamplingX;
												int pyv2 = (py % mcuH) * v2 / maxSamplingY;
												int nxh2 = (nx % mcuW) * h2 / maxSamplingX;
												int nyv2 = (ny % mcuH) * v2 / maxSamplingY;

												int c00 = coefficients[py / mcuH][px / mcuW][c2 + (pxh2 / 8) + h2 * (pyv2 / 8)][(pxh2 % 8) + 8 * (pyv2 % 8)];
												int c10 = coefficients[py / mcuH][ix / mcuW][c2 + (ixh2 / 8) + h2 * (pyv2 / 8)][(ixh2 % 8) + 8 * (pyv2 % 8)];
												int c20 = coefficients[py / mcuH][nx / mcuW][c2 + (nxh2 / 8) + h2 * (pyv2 / 8)][(nxh2 % 8) + 8 * (pyv2 % 8)];

												int c01 = coefficients[iy / mcuH][px / mcuW][c2 + (pxh2 / 8) + h2 * (iyv2 / 8)][(pxh2 % 8) + 8 * (iyv2 % 8)];
												int c11 = cr11;
												int c21 = coefficients[iy / mcuH][nx / mcuW][c2 + (nxh2 / 8) + h2 * (iyv2 / 8)][(nxh2 % 8) + 8 * (iyv2 % 8)];

												int c02 = coefficients[ny / mcuH][px / mcuW][c2 + (pxh2 / 8) + h2 * (nyv2 / 8)][(pxh2 % 8) + 8 * (nyv2 % 8)];
												int c12 = coefficients[ny / mcuH][ix / mcuW][c2 + (ixh2 / 8) + h2 * (nyv2 / 8)][(ixh2 % 8) + 8 * (nyv2 % 8)];
												int c22 = coefficients[ny / mcuH][nx / mcuW][c2 + (nxh2 / 8) + h2 * (nyv2 / 8)][(nxh2 % 8) + 8 * (nyv2 % 8)];

												int z = (ix & 1) + 2 * (iy & 1);
												cr11 = (int)((w[z][0][0]*c00 + w[z][0][1]*c10 + w[z][0][2]*c20 + w[z][1][0]*c01 + w[z][1][1]*c11 + w[z][1][2]*c21 + w[z][2][0]*c02 + w[z][2][1]*c12 + w[z][2][2]*c22) / (1+5*0.25+3*0.5));

//												cr11 = (int)((c11 + 0.06 * (c00+c20+c02+c22) + 0.33 * (c01+c10+c20+c02)) / (1+0.06*4+0.33*4));
//												cr11 = (int)((c11 + 0.25 * (c00+c20+c02+c22) + 0.50 * (c01+c10+c20+c02)) / (1+0.25*4+0.50*4));
//												cr11 = (c00 + c10 + c20 + c01 + c11 + c21 + c02 + c12 + c22) / 9;
											}
										}

//										mImage.getRaster()[iy * mJPEG.width + ix] = 0xff000000 | (lu11 << 16) + (lu11 << 8) + lu11;
//										mImage.getRaster()[iy * mJPEG.width + ix] = 0xff000000 | (cr11 << 16) + (cr11 << 8) + cr11;
										
										if (mJPEG.mColorSpace == ColorSpaceType.YCBCR)
										{
											mImage.getRaster()[iy * mJPEG.width + ix] = ColorSpace.yuvToRgbFloat(lu11, cb11, cr11);
										}
										else if (mJPEG.mColorSpace == ColorSpaceType.RGB)
										{
											mImage.getRaster()[iy * mJPEG.width + ix] = 0xff000000 | (lu11 << 16) + (cb11 << 8) + cr11;
										}
										else
										{
											throw new IllegalStateException("Unsupported color space: " + mJPEG.mColorSpace);
										}
									}
								}
//							}
//						}
//					}
//				}
			}
		}
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
}
