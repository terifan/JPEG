package org.terifan.imageio.jpeg.encoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP2Segment;
import org.terifan.imageio.jpeg.ColorSpace;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.DQTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.SOSSegment;


public class JPEGImageWriter
{
	private BitOutputStream mBitStream;
	private JPEG mJPEG;
	private ProgressionScript mProgressionScript;


	public JPEGImageWriter(OutputStream aOutputStream)
	{
		mJPEG = new JPEG();

		mBitStream = new BitOutputStream(aOutputStream);
	}


	public void write(BufferedImage aImage, int aQuality) throws IOException
	{
		mJPEG.width = aImage.getWidth();
		mJPEG.height = aImage.getHeight();

		ComponentInfo lu = new ComponentInfo(ComponentInfo.Y, 1, 0, 2, 2);
		ComponentInfo cb = new ComponentInfo(ComponentInfo.CB, 2, 1, 1, 1);
		ComponentInfo cr = new ComponentInfo(ComponentInfo.CR, 3, 1, 1, 1);

		mJPEG.components = new ComponentInfo[]{lu, cb, cr};
		mJPEG.num_components = mJPEG.components.length;
		mJPEG.mSOFSegment = new SOFSegment(mJPEG, mJPEG.width, mJPEG.height, 8, mJPEG.components);

		mJPEG.mQuantizationTables = new QuantizationTable[2];
		mJPEG.mQuantizationTables[0] = QuantizationTableFactory.buildQuantTable(aQuality, 0);
		mJPEG.mQuantizationTables[1] = QuantizationTableFactory.buildQuantTable(aQuality, 1);

		sampleImage(mJPEG.mSOFSegment, aImage, new FDCTFloat());

		create(mJPEG);

		encodeCoefficients(mJPEG);

		mBitStream.writeInt16(JPEGConstants.EOI);
	}


	public void create(JPEG aJPEG) throws IOException
	{
		mBitStream.writeInt16(JPEGConstants.SOI);

		new APP0Segment(aJPEG).write(mBitStream);

		if (aJPEG.mICCProfile != null)
		{
			new APP2Segment(aJPEG).setType(APP2Segment.ICC_PROFILE).write(mBitStream);
		}

		new DQTSegment(aJPEG).write(mBitStream);

		aJPEG.mSOFSegment.write(mBitStream);
	}


	private void sampleImage(SOFSegment aSOFSegment, BufferedImage aImage, FDCT aFdct) throws UnsupportedOperationException
	{
		int maxSamplingX = aSOFSegment.getMaxHorSampling();
		int maxSamplingY = aSOFSegment.getMaxVerSampling();
		int numHorMCU = aSOFSegment.getHorMCU();
		int numVerMCU = aSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		int blocks_in_MCU = 0;
		for (int ci = 0; ci < mJPEG.num_components; ci++)
		{
			ComponentInfo comp = aSOFSegment.getComponent(ci);
			blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		mJPEG.mCoefficients = new int[numVerMCU][numHorMCU][blocks_in_MCU][64];

		int[] raster = new int[mcuWidth * mcuHeight];
		int[][] colors = new int[mJPEG.num_components][mcuWidth * mcuHeight];

		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
				int bx = mcuX * mcuWidth;
				int by = mcuY * mcuHeight;

				aImage.getRGB(bx, by, mcuWidth, mcuHeight, raster, 0, mcuWidth);

				ColorSpace.YCBCR.rgbToYuv(raster, colors[0], colors[1], colors[2]);

				for (int ci = 0, blockIndex = 0; ci < mJPEG.num_components; ci++)
				{
					ComponentInfo comp = aSOFSegment.getComponent(ci);
					int samplingX = comp.getHorSampleFactor();
					int samplingY = comp.getVerSampleFactor();

					QuantizationTable quantizationTable = mJPEG.mQuantizationTables[comp.getQuantizationTableId()];

					for (int blockY = 0; blockY < samplingY; blockY++)
					{
						for (int blockX = 0; blockX < samplingX; blockX++, blockIndex++)
						{
							if (samplingX == 1 && samplingY == 1 && maxSamplingX == 2 && maxSamplingY == 2)
							{
								downsample16x16(mJPEG.mCoefficients[mcuY][mcuX][blockIndex], colors[ci]);
							}
							else if (samplingX == 2 && samplingY == 1 && maxSamplingX == 2 && maxSamplingY == 2)
							{
								downsample8x16(mJPEG.mCoefficients[mcuY][mcuX][blockIndex], colors[ci], 8 * blockX);
							}
							else if (samplingX == 2 && samplingY == 2)
							{
								copyBlock(mJPEG.mCoefficients[mcuY][mcuX][blockIndex], colors[ci], 8 * blockX, 8 * blockY, mcuWidth);
							}
							else
							{
								throw new UnsupportedOperationException(samplingX+" "+samplingY+" "+maxSamplingX+" "+maxSamplingY);
							}
							aFdct.transform(mJPEG.mCoefficients[mcuY][mcuX][blockIndex], quantizationTable);
						}
					}
				}
			}
		}
	}


	public void finish(JPEG aJPEG) throws IOException
	{
		mBitStream.writeInt16(JPEGConstants.EOI);
	}


	public void encodeCoefficients(JPEG aJPEG) throws IOException
	{
		if (aJPEG.mProgressive && mProgressionScript == null)
		{
			mProgressionScript = new ProgressionScript(JPEGConstants.DEFAULT_PROGRESSION_SCRIPT);

////			String s = 
////				"0,1,2: 0-0,   0, 0 ;\n" +
////				"0:     1-63,  0, 0 ;\n" +
////				"2:     1-63,  0, 0 ;\n" +
////				"1:     1-63,  0, 0 ;\n";
//
//			String s
//				= "0,1,2: 0-0,   0, 0 ;\n"
//				+ "0:     1-5,  0, 0 ;\n"
//				+ "0:     6-63,  0, 0 ;\n"
//				+ "2:     1-63,  0, 0 ;\n"
//				+ "1:     1-63,  0, 0 ;\n"
//
////				= "0,1,2: 0-0,   0, 1 ;\n"
////				+ "0,1,2: 0-0,   1, 0 ;\n"
//
////				+ "2:     1-63,  0, 0 ;\n"
////				+ "1:     1-63,  0, 0 ;\n"
////
////				+ "0:     1-5,   0, 0 ;\n"
////				+ "0:     6-63,  0, 0 ;\n"
//				
////				+ "0:     1-5,   0, 2 ;\n"
////				+ "2:     1-63,  0, 1 ;\n"
////				+ "1:     1-63,  0, 1 ;\n"
////				+ "0:     6-63,  0, 2 ;\n"
////				+ "0:     1-63,  2, 1 ;\n"
////				+ "2:     1-63,  1, 0 ;\n"
////				+ "1:     1-63,  1, 0 ;\n"
////				+ "0:     1-63,  1, 0 ;\n"
//				;
//
//			mProgressionScript = new ProgressionScript(s);
		}

		aJPEG.num_hor_mcu = aJPEG.mSOFSegment.getHorMCU();
		aJPEG.num_ver_mcu = aJPEG.mSOFSegment.getVerMCU();

		System.out.println("----"+aJPEG.mProgressive);

			Encoder encoder = null;
		for (int progressionLevel = 0; progressionLevel < (aJPEG.mProgressive ? mProgressionScript.getParams().size() : 1); progressionLevel++)
		{

			System.out.println("LEVEL " + progressionLevel);

			SOSSegment sosSegment;

			if (aJPEG.mProgressive)
			{
				int[][] params = mProgressionScript.getParams().get(progressionLevel);

				aJPEG.Ss = params[1][0];
				aJPEG.Se = params[1][1];
				aJPEG.Ah = params[1][2];
				aJPEG.Al = params[1][3];

				int[] id = new int[params[0].length];
				for (int i = 0; i < params[0].length; i++)
				{
					id[i] = aJPEG.mSOFSegment.getComponent(params[0][i]).getComponentId();
				}

				sosSegment = new SOSSegment(aJPEG, id);

				if (params[0].length == 3)
				{
					if (progressionLevel == 0)
					{
						sosSegment.setTableDC(0, 0);
						sosSegment.setTableAC(0, 0);
						sosSegment.setTableDC(1, 1);
						sosSegment.setTableAC(1, 0);
						sosSegment.setTableDC(2, 1);
						sosSegment.setTableAC(2, 0);
					}
					else
					{
						sosSegment.setTableDC(0, 0);
						sosSegment.setTableAC(0, 0);
						sosSegment.setTableDC(1, 0);
						sosSegment.setTableAC(1, 0);
						sosSegment.setTableDC(2, 0);
						sosSegment.setTableAC(2, 0);
					}
				}
				else
				{
					sosSegment.setTableDC(0, 0);
					sosSegment.setTableAC(0, id[0] == 1 ? 0 : 1);
				}

				sosSegment.prepareMCU();
//
//				if (JPEGConstants.VERBOSE)
//				{
//					System.out.println("  {ss=" + aJPEG.Ss + ", se=" + aJPEG.Se + ", ah=" + aJPEG.Ah + ", al=" + aJPEG.Al + "}");
//				}
			}
			else
			{
				aJPEG.Ss = 0;
				aJPEG.Se = 63;
				aJPEG.Ah = 0;
				aJPEG.Al = 0;

				sosSegment = new SOSSegment(aJPEG, aJPEG.mSOFSegment.getComponentIds());
				sosSegment.setTableDC(0, 0);
				sosSegment.setTableAC(0, 0);
				sosSegment.setTableDC(1, 1);
				sosSegment.setTableAC(1, 1);
				sosSegment.setTableDC(2, 1);
				sosSegment.setTableAC(2, 1);

				sosSegment.prepareMCU();
			}

			int[][] mcu = new int[aJPEG.blocks_in_MCU][64];

			int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
			int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
			int mcuWidth = 8 * maxSamplingX;
			int mcuHeight = 8 * maxSamplingY;
			
			if (aJPEG.mArithmetic)
			{
				boolean dac = true;
				if (progressionLevel == 0)
				{
					aJPEG.arith_dc_L = new int[]{0,0};
					aJPEG.arith_dc_U = new int[]{1,1};
					aJPEG.arith_ac_K = new int[]{};
				}
				else if (progressionLevel == 1)
				{
					aJPEG.arith_dc_L = new int[]{};
					aJPEG.arith_dc_U = new int[]{};
					aJPEG.arith_ac_K = new int[]{5,5};
				}
				else if (progressionLevel == 2)
				{
					aJPEG.arith_dc_L = new int[]{};
					aJPEG.arith_dc_U = new int[]{};
					aJPEG.arith_ac_K = new int[]{5,5};
				}
				else if (progressionLevel == 3)
				{
					aJPEG.arith_dc_L = new int[]{};
					aJPEG.arith_dc_U = new int[]{};
					aJPEG.arith_ac_K = new int[]{5,5};
				}
				else if (progressionLevel == 4)
				{
					aJPEG.arith_dc_L = new int[]{};
					aJPEG.arith_dc_U = new int[]{};
					aJPEG.arith_ac_K = new int[]{5,5};
				}
				else if (progressionLevel == 5)
				{
					aJPEG.arith_dc_L = new int[]{};
					aJPEG.arith_dc_U = new int[]{};
					aJPEG.arith_ac_K = new int[]{5,5};
				}
				else if (progressionLevel == 6)
				{
					dac = false;
				}
				else
				{
					aJPEG.arith_dc_L = new int[]{};
					aJPEG.arith_dc_U = new int[]{};
					aJPEG.arith_ac_K = new int[]{5,5};
				}
				
				if (dac)
				{
					new DACSegment(aJPEG).write(mBitStream, sosSegment);
				}

				if (encoder == null)
				{
					encoder = new ArithmeticEncoder(mBitStream);

					encoder.jinit_encoder(aJPEG);
				}
			}
			else
			{
				if (encoder == null)
				{
					encoder = new HuffmanEncoder(mBitStream);

					encoder.jinit_encoder(aJPEG);
				}

				if (aJPEG.mOptimizedHuffman || aJPEG.mProgressive)
				{
					encoder.start_pass(aJPEG, true);

					if (aJPEG.comps_in_scan == 1)
					{
						ComponentInfo comp = aJPEG.cur_comp_info[0];

						for (int mcuY = 0; mcuY < aJPEG.num_ver_mcu; mcuY++)
						{
							for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
							{
								if (mcuY < aJPEG.num_ver_mcu - 1 || mcuY * mcuHeight + blockY * 8 < aJPEG.height)
								{
									for (int mcuX = 0; mcuX < aJPEG.num_hor_mcu; mcuX++)
									{
										for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
										{
											if (mcuX < aJPEG.num_hor_mcu - 1 || mcuX * mcuWidth + blockX * 8 < aJPEG.width)
											{
												mcu[0] = aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

												encoder.encode_mcu(aJPEG, mcu, true);
											}
										}
									}
								}
							}
						}
					}
					else
					{
						for (int mcuY = 0; mcuY < aJPEG.num_ver_mcu; mcuY++)
						{
							for (int mcuX = 0; mcuX < aJPEG.num_hor_mcu; mcuX++)
							{
								encoder.encode_mcu(aJPEG, aJPEG.mCoefficients[mcuY][mcuX], true);
							}
						}
					}

					encoder.finish_pass(aJPEG, true);
				}
				else
				{
					HuffmanEncoder.setupStandardHuffmanTables(aJPEG);
				}

				new DHTSegment(aJPEG).write(mBitStream);
			}

			if (JPEGConstants.VERBOSE)
			{
				System.out.println("  {ss=" + aJPEG.Ss + ", se=" + aJPEG.Se + ", ah=" + aJPEG.Ah + ", al=" + aJPEG.Al + "}");
			}

			sosSegment.write(mBitStream);

			encoder.start_pass(aJPEG, false);

			if (aJPEG.comps_in_scan == 1)
			{
				ComponentInfo comp = aJPEG.cur_comp_info[0];

				for (int mcuY = 0; mcuY < aJPEG.num_ver_mcu; mcuY++)
				{
					for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
					{
						if (mcuY < aJPEG.num_ver_mcu - 1 || mcuY * mcuHeight + blockY * 8 < aJPEG.height)
						{
							for (int mcuX = 0; mcuX < aJPEG.num_hor_mcu; mcuX++)
							{
								for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
								{
									if (mcuX < aJPEG.num_hor_mcu - 1 || mcuX * mcuWidth + blockX * 8 < aJPEG.width)
									{
										mcu[0] = aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

										encoder.encode_mcu(aJPEG, mcu, false);
									}
								}
							}
						}
					}
				}
			}
			else
			{
				for (int mcuY = 0; mcuY < aJPEG.num_ver_mcu; mcuY++)
				{
					for (int mcuX = 0; mcuX < aJPEG.num_hor_mcu; mcuX++)
					{
						encoder.encode_mcu(aJPEG, aJPEG.mCoefficients[mcuY][mcuX], false);
					}
				}
			}

			encoder.finish_pass(aJPEG, false);
			
			System.out.println(mBitStream.getStreamOffset());
		}
	}


	private void downsample16x16(int[] aDst, int[] aSrc)
	{
		for (int y = 0, i = 0; y < 8; y++)
		{
			for (int x = 0; x < 8; x++, i++)
			{
				int v =
					  aSrc[16 * (2 * y + 0) + 2 * x + 0]
					+ aSrc[16 * (2 * y + 0) + 2 * x + 1]
					+ aSrc[16 * (2 * y + 1) + 2 * x + 0]
					+ aSrc[16 * (2 * y + 1) + 2 * x + 1];

				aDst[i] = (v + 2) / 4;
			}
		}
	}


	private void downsample8x16(int[] aDst, int[] aSrc, int aOffsetX)
	{
		for (int y = 0; y < 8; y++)
		{
			for (int x = 0; x < 8; x++)
			{
				int v =
					  aSrc[16 * (2 * y + 0) + aOffsetX + x]
					+ aSrc[16 * (2 * y + 1) + aOffsetX + x];

				aDst[y * 8 + x] = (v + 1) / 2;
			}
		}
	}


	private void copyBlock(int[] aDst, int[] aSrc, int aSrcOffsetX, int aSrcOffsetY, int aSrcWidth)
	{
		for (int y = 0; y < 8; y++)
		{
			System.arraycopy(aSrc, aSrcOffsetX + aSrcWidth * (aSrcOffsetY + y), aDst, 8 * y, 8);
		}
	}


	public JPEGImageWriter setOptimizedHuffman(boolean aOptimizedHuffman)
	{
		mJPEG.mOptimizedHuffman = aOptimizedHuffman;
		return this;
	}


	public JPEGImageWriter setProgressive(boolean aProgressive)
	{
		mJPEG.mProgressive = aProgressive;
		return this;
	}


	public JPEGImageWriter setArithmetic(boolean aArithmetic)
	{
		mJPEG.mArithmetic = aArithmetic;
		return this;
	}
}
