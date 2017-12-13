package org.terifan.imageio.jpeg.encoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.ColorSpace;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DQTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.SOSSegment;
import org.terifan.imageio.jpeg.test.Debug;


public class JPEGImageWriter
{
	private BitOutputStream mBitStream;
	private JPEG mJPEG;


	public void write(BufferedImage aImage, int aQuality, OutputStream aOutputStream) throws IOException
	{
		mJPEG = new JPEG();

		mJPEG.mQuantizationTables = new QuantizationTable[2];
		mJPEG.mQuantizationTables[0] = QuantizationTableFactory.buildQuantTable(aQuality, 0);
		mJPEG.mQuantizationTables[1] = QuantizationTableFactory.buildQuantTable(aQuality, 1);
		mJPEG.mArithmetic = true;

		mJPEG.arith_dc_L = new int[]{0,0};
		mJPEG.arith_dc_U = new int[]{1,1};
		mJPEG.arith_ac_K = new int[]{5,5};

		mBitStream = new BitOutputStream(aOutputStream);

		mBitStream.writeInt16(JPEGConstants.SOI);

		new APP0Segment(mJPEG).write(mBitStream);

		new DQTSegment(mJPEG).write(mBitStream);

		ComponentInfo lu, cb, cr;

		SOFSegment mSOFSegment = new SOFSegment(mJPEG, aImage.getWidth(), aImage.getHeight(), 8,
			lu=new ComponentInfo(ComponentInfo.Y, 1, 0, 2, 2),
			cb=new ComponentInfo(ComponentInfo.CB, 2, 1, 2, 1),
			cr=new ComponentInfo(ComponentInfo.CR, 3, 1, 2, 1)
		).write(mBitStream);

		new DACSegment(mJPEG).write(mBitStream);

		mJPEG.num_components = 3;
		mJPEG.comps_in_scan = 3;
		mJPEG.cur_comp_info = new ComponentInfo[]{lu,cb,cr};
		mJPEG.Ss = 0;
		mJPEG.Se = 63;
		mJPEG.Ah = 0;
		mJPEG.Al = 0;

		mJPEG.cur_comp_info[0].setTableDC(0);
		mJPEG.cur_comp_info[0].setTableAC(0);
		mJPEG.cur_comp_info[1].setTableDC(1);
		mJPEG.cur_comp_info[1].setTableAC(1);
		mJPEG.cur_comp_info[2].setTableDC(1);
		mJPEG.cur_comp_info[2].setTableAC(1);

		new SOSSegment(mJPEG).write(mBitStream);

		mJPEG.blocks_in_MCU = 0;
		for (int scanComponentIndex = 0; scanComponentIndex < mJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = mJPEG.cur_comp_info[scanComponentIndex];
			mJPEG.blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		mJPEG.MCU_membership = new int[mJPEG.blocks_in_MCU];
		mJPEG.MCU_membership[0] = 0;
		mJPEG.MCU_membership[1] = 0;
		mJPEG.MCU_membership[2] = 0;
		mJPEG.MCU_membership[3] = 0;
		mJPEG.MCU_membership[4] = 1;
		mJPEG.MCU_membership[5] = 2;

		int maxSamplingX = mSOFSegment.getMaxHorSampling();
		int maxSamplingY = mSOFSegment.getMaxVerSampling();
		int numHorMCU = mSOFSegment.getHorMCU();
		int numVerMCU = mSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		FDCT fdct = new FDCTFloat();

		int[][][][] buffer = new int[numVerMCU][numHorMCU][mJPEG.blocks_in_MCU][64];

		int[] raster = new int[mcuWidth * mcuHeight];
		int[][] colors = new int[mJPEG.num_components][mcuWidth * mcuHeight];

		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
				int bx = mcuX * mcuWidth;
				int by = mcuY * mcuHeight;

				aImage.getRGB(bx, by, mcuWidth, mcuHeight, raster, 0, mcuWidth);

				ColorSpace.rgbToYuvFloat(raster, colors[0], colors[1], colors[2]);

				for (int componentIndex = 0, blockIndex = 0; componentIndex < mJPEG.comps_in_scan; componentIndex++)
				{
					ComponentInfo comp = mSOFSegment.getComponent(componentIndex);

					int samplingX = comp.getHorSampleFactor();
					int samplingY = comp.getVerSampleFactor();

					QuantizationTable quantizationTable = mJPEG.mQuantizationTables[comp.getQuantizationTableId()];

					for (int blockY = 0; blockY < samplingY; blockY++)
					{
						for (int blockX = 0; blockX < samplingX; blockX++, blockIndex++)
						{
							if (samplingX == 1 && samplingY == 1 && maxSamplingX == 2 && maxSamplingY == 2)
							{
								downsample16x16(buffer[mcuY][mcuX][blockIndex], colors[componentIndex]);
							}
							else if (samplingX == 2 && samplingY == 1 && maxSamplingX == 2 && maxSamplingY == 2)
							{
								downsample8x16(buffer[mcuY][mcuX][blockIndex], colors[componentIndex], 8 * blockX);
							}
							else if (samplingX == 2 && samplingY == 2)
							{
								copyBlock(buffer[mcuY][mcuX][blockIndex], colors[componentIndex], 8 * blockX, 8 * blockY, mcuWidth);
							}
							else
							{
								throw new UnsupportedOperationException(samplingX+" "+samplingY+" "+maxSamplingX+" "+maxSamplingY);
							}

							fdct.transform(buffer[mcuY][mcuX][blockIndex], quantizationTable);
						}
					}
				}

				if (mcuY == 0 && mcuX == 0)
				{
					System.out.println("WRITER " + mJPEG.mArithmetic);
					Debug.printTables(buffer[mcuY][mcuX]);
					System.out.println();
				}
			}
		}



		ArithmeticEncoder encoder = new ArithmeticEncoder(mBitStream);
		encoder.jinit_encoder(mJPEG);
		encoder.start_pass(mJPEG, false);

		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
				encoder.encode_mcu(mJPEG, buffer[mcuY][mcuX]);
			}
		}

		encoder.finish_pass(mJPEG);

		mBitStream.writeInt16(JPEGConstants.EOI);
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
}
