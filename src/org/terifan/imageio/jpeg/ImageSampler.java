package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.encoder.FDCT;


public class ImageSampler
{
	public static void sampleImage(JPEG aJPEG, BufferedImage aImage, FDCT aFDCT) throws UnsupportedOperationException
	{
		ColorSpace colorSpace = ColorSpace.YCBCR;
		int numComponents = aJPEG.mSOFSegment.getComponents().length;
		int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
		int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();
		int numVerMCU = aJPEG.mSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		int blocks_in_MCU = 0;
		for (int ci = 0; ci < numComponents; ci++)
		{
			ComponentInfo comp = aJPEG.mSOFSegment.getComponent(ci);
			blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		aJPEG.mCoefficients = new int[numVerMCU][numHorMCU][blocks_in_MCU][64];

		int[] raster = new int[mcuWidth * mcuHeight];
		int[][] colors = new int[numComponents][mcuWidth * mcuHeight];

		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
				int bx = mcuX * mcuWidth;
				int by = mcuY * mcuHeight;

				aImage.getRGB(bx, by, mcuWidth, mcuHeight, raster, 0, mcuWidth);

				colorSpace.rgbToYuv(raster, colors[0], colors[1], colors[2]);

				for (int ci = 0, blockIndex = 0; ci < numComponents; ci++)
				{
					ComponentInfo comp = aJPEG.mSOFSegment.getComponent(ci);
					int samplingX = comp.getHorSampleFactor();
					int samplingY = comp.getVerSampleFactor();

					QuantizationTable quantizationTable = aJPEG.mQuantizationTables[comp.getQuantizationTableId()];

					for (int blockY = 0; blockY < samplingY; blockY++)
					{
						for (int blockX = 0; blockX < samplingX; blockX++, blockIndex++)
						{
							if (samplingX == 1 && samplingY == 1 && maxSamplingX == 2 && maxSamplingY == 2)
							{
								downsample16x16(aJPEG.mCoefficients[mcuY][mcuX][blockIndex], colors[ci]);
							}
							else if (samplingX == 2 && samplingY == 1 && maxSamplingX == 2 && maxSamplingY == 2)
							{
								downsample8x16(aJPEG.mCoefficients[mcuY][mcuX][blockIndex], colors[ci], 8 * blockX);
							}
							else if (samplingX == 2 && samplingY == 2)
							{
								copyBlock(aJPEG.mCoefficients[mcuY][mcuX][blockIndex], colors[ci], 8 * blockX, 8 * blockY, mcuWidth);
							}
							else
							{
								throw new UnsupportedOperationException(samplingX+" "+samplingY+" "+maxSamplingX+" "+maxSamplingY);
							}
							aFDCT.transform(aJPEG.mCoefficients[mcuY][mcuX][blockIndex], quantizationTable);
						}
					}
				}
			}
		}
	}


	private static void downsample16x16(int[] aDst, int[] aSrc)
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


	private static void downsample8x16(int[] aDst, int[] aSrc, int aOffsetX)
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


	private static void copyBlock(int[] aDst, int[] aSrc, int aSrcOffsetX, int aSrcOffsetY, int aSrcWidth)
	{
		for (int y = 0; y < 8; y++)
		{
			System.arraycopy(aSrc, aSrcOffsetX + aSrcWidth * (aSrcOffsetY + y), aDst, 8 * y, 8);
		}
	}
}
