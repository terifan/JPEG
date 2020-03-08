package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.terifan.imageio.jpeg.decoder.IDCT;


public class ImageTransdecode
{
	public static void transform(JPEG aJPEG, IDCT aIDCT, BufferedImage aImage)
	{
		ComponentInfo[] components = aJPEG.mSOFSegment.getComponents();
		int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
		int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();
		int numVerMCU = aJPEG.mSOFSegment.getVerMCU();
		int numComponents = components.length;

		int mcuW = 8 * maxSamplingX;
		int mcuH = 8 * maxSamplingY;

		int[][][][] coefficients = aJPEG.mCoefficients;

		int[] blockLookup = new int[12];

		int cp = 0;
		int cii = 0;
		for (ComponentInfo ci : components)
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
					ComponentInfo comp = components[blockLookup[blockIndex]];

					QuantizationTable quantizationTable = aJPEG.mQuantizationTables[comp.getQuantizationTableId()];

					aIDCT.transform(coefficients[mcuY][mcuX][blockIndex], quantizationTable);
				}
			}
		}

		int c0 = components[0].getComponentBlockOffset();
		int c1 = components.length == 1 ? 0 : components[1].getComponentBlockOffset();
		int c2 = components.length == 1 ? 0 : components[2].getComponentBlockOffset();

		int h0 = components[0].getHorSampleFactor();
		int v0 = components[0].getVerSampleFactor();
		int h1 = components.length == 1 ? 0 : components[1].getHorSampleFactor();
		int v1 = components.length == 1 ? 0 : components[1].getVerSampleFactor();
		int h2 = components.length == 1 ? 0 : components[2].getHorSampleFactor();
		int v2 = components.length == 1 ? 0 : components[2].getVerSampleFactor();

		if (components.length == 1 && (h0 != 1 || v0 != 1))
		{
			throw new IllegalStateException("Unsupported subsampling");
		}

		for (int iy = 0; iy < aJPEG.mSOFSegment.getHeight(); iy++)
		{
			for (int ix = 0; ix < aJPEG.mSOFSegment.getWidth(); ix++)
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

									if (numComponents == 1)
									{
										color = aJPEG.mColorSpace.yccToRgb(lu, 0, 0);
									}
									else if (numComponents == 3)
									{
										int cb;
										int cr;

										if (h0 == 2 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
										{
											cb = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_3X3);
											cr = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_3X3);
										}
										else if (h0 == 1 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
										{
											cb = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_1X3);
											cr = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_1X3);
										}
										else if (h0 == 2 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
										{
											cb = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_3X1);
											cr = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_3X1);
										}
										else
										{
											cb = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_1X1);
											cr = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_1X1);
										}

										color = aJPEG.mColorSpace.yccToRgb(lu, cb, cr);
									}
									else
									{
										throw new IllegalStateException();
									}

									aImage.setRGB(ix, iy, color);
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


	private static int upsampleChroma(JPEG aJPEG, int ix, int iy, int aMcuW, int aMcuH, int aSamplingHor, int aSamplingVer, int aMaxSamplingX, int aMaxSamplingY, int[][][][] aCoefficients, int aComponentOffset, int[][] kernel)
	{
		int sum = 0;
		int total = 0;

		int x = ix - kernel[0].length / 2;
		int y = iy - kernel.length / 2;
		int iw = aJPEG.mSOFSegment.getWidth();
		int ih = aJPEG.mSOFSegment.getHeight();

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
}
