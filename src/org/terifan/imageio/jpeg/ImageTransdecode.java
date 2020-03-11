package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.terifan.imageio.jpeg.decoder.IDCT;


public class ImageTransdecode
{
	public static void transform(JPEG aJPEG, IDCT aIDCT, BufferedImage aImage, int aMCUX, int aMCUY)
	{
		SOFSegment sof = aJPEG.mSOFSegment;
		ComponentInfo[] components = sof.getComponents();
		int maxSamplingX = sof.getMaxHorSampling();
		int maxSamplingY = sof.getMaxVerSampling();
		int numHorMCU = sof.getHorMCU();
		int numVerMCU = sof.getVerMCU();
		int numComponents = components.length;

		int mcuW = 8 * maxSamplingX;
		int mcuH = 8 * maxSamplingY;

		int[][] coefficients = aJPEG.mCoefficients[aMCUY][aMCUX];

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

		int[] rgb = new int[mcuW * mcuH];

		int imageW = sof.getWidth();
		int imageH = sof.getHeight();

		if (numComponents == 1)
		{
		}
		else if (numComponents == 3)
		{
		}

		for (int blockIndex = 0; blockIndex < sof.getBlocksInMCU(); blockIndex++)
		{
			ComponentInfo comp = sof.getComponentByBlockIndex(blockIndex);

			QuantizationTable quantizationTable = aJPEG.mQuantizationTables[comp.getQuantizationTableId()];

			aIDCT.transform(coefficients[blockIndex], quantizationTable);
		}

		ColorSpace colorSpace = aJPEG.mColorSpace;

		if (h0 == 1 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:4:4
		{
			for (int iy = 0, i = 0; iy < 8; iy++)
			{
				for (int ix = 0; ix < 8; ix++, i++)
				{
					int lu = coefficients[c0][i];
					int cb = coefficients[c1][i];
					int cr = coefficients[c2][i];

					rgb[i] = colorSpace.yccToRgb(lu, cb, cr);
				}
			}
		}
		else if (h0 == 2 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:2:0
		{
			for (int by = 0; by < 2; by++)
			{
				for (int bx = 0; bx < 2; bx++)
				{
					for (int iy = 0; iy < 4; iy++)
					{
						for (int ix = 0; ix < 4; ix++)
						{
							int lu0 = coefficients[c0+by*2+bx][iy * 2 * 8 + 0 + 2 * ix + 0];
							int lu1 = coefficients[c0+by*2+bx][iy * 2 * 8 + 0 + 2 * ix + 1];
							int lu2 = coefficients[c0+by*2+bx][iy * 2 * 8 + 8 + 2 * ix + 0];
							int lu3 = coefficients[c0+by*2+bx][iy * 2 * 8 + 8 + 2 * ix + 1];

							int cb = coefficients[c1][by * 4 * 8 + iy * 8 + 4 * bx + ix];
							int cr = coefficients[c2][by * 4 * 8 + iy * 8 + 4 * bx + ix];

							rgb[by * 8 * 16 + bx * 8 + (2*iy + 0) * 16 + ix * 2 + 0] = colorSpace.yccToRgb(lu0, cb, cr);
							rgb[by * 8 * 16 + bx * 8 + (2*iy + 0) * 16 + ix * 2 + 1] = colorSpace.yccToRgb(lu1, cb, cr);
							rgb[by * 8 * 16 + bx * 8 + (2*iy + 1) * 16 + ix * 2 + 0] = colorSpace.yccToRgb(lu2, cb, cr);
							rgb[by * 8 * 16 + bx * 8 + (2*iy + 1) * 16 + ix * 2 + 1] = colorSpace.yccToRgb(lu3, cb, cr);
						}
					}
				}
			}
		}
		else if (h0 == 2 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:2:2
		{
			for (int bx = 0; bx < 2; bx++)
			{
				for (int iy = 0; iy < 8; iy++)
				{
					for (int ix = 0; ix < 4; ix++)
					{
						int lu0 = coefficients[c0+bx][iy * 8 + 2 * ix + 0];
						int lu1 = coefficients[c0+bx][iy * 8 + 2 * ix + 1];

						int cb = coefficients[c1][iy * 8 + 4 * bx + ix];
						int cr = coefficients[c2][iy * 8 + 4 * bx + ix];

						rgb[bx * 8 + iy * 16 + ix * 2 + 0] = colorSpace.yccToRgb(lu0, cb, cr);
						rgb[bx * 8 + iy * 16 + ix * 2 + 1] = colorSpace.yccToRgb(lu1, cb, cr);
					}
				}
			}
		}
		else if (h0 == 2 && v0 == 2 && h1 == 2 && v1 == 1 && h2 == 2 && v2 == 1) // 4:4:0
		{
			for (int bx = 0; bx < 2; bx++)
			{
				for (int by = 0; by < 2; by++)
				{
					for (int iy = 0; iy < 4; iy++)
					{
						for (int ix = 0; ix < 8; ix++)
						{
							int lu0 = coefficients[c0+by*2+bx][iy * 2 * 8 + 0 + ix];
							int lu1 = coefficients[c0+by*2+bx][iy * 2 * 8 + 8 + ix];

							int cb = coefficients[c1 + bx][by * 4 * 8 + iy * 8 + ix];
							int cr = coefficients[c2 + bx][by * 4 * 8 + iy * 8 + ix];

							rgb[by * 8 * 16 + (2 * iy + 0) * 16 + 8 * bx + ix] = colorSpace.yccToRgb(lu0, cb, cr);
							rgb[by * 8 * 16 + (2 * iy + 1) * 16 + 8 * bx + ix] = colorSpace.yccToRgb(lu1, cb, cr);
						}
					}
				}
			}
		}
		else if (h0 == 4 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:1:1
		{
			for (int bx = 0; bx < 4; bx++)
			{
				for (int iy = 0; iy < 8; iy++)
				{
					for (int ix = 0; ix < 2; ix++)
					{
						int lu0 = coefficients[c0+bx][iy * 8 + 4 * ix + 0];
						int lu1 = coefficients[c0+bx][iy * 8 + 4 * ix + 1];
						int lu2 = coefficients[c0+bx][iy * 8 + 4 * ix + 2];
						int lu3 = coefficients[c0+bx][iy * 8 + 4 * ix + 3];

						int cb = coefficients[c1][iy * 8 + bx * 2 + ix];
						int cr = coefficients[c2][iy * 8 + bx * 2 + ix];

						rgb[32 * iy + 8 * bx + 4 * ix + 0] = colorSpace.yccToRgb(lu0, cb, cr);
						rgb[32 * iy + 8 * bx + 4 * ix + 1] = colorSpace.yccToRgb(lu1, cb, cr);
						rgb[32 * iy + 8 * bx + 4 * ix + 2] = colorSpace.yccToRgb(lu2, cb, cr);
						rgb[32 * iy + 8 * bx + 4 * ix + 3] = colorSpace.yccToRgb(lu3, cb, cr);
					}
				}
			}
		}

		aImage.setRGB(aMCUX * mcuW, aMCUY * mcuH, Math.min(mcuW, imageW - aMCUX * mcuW), Math.min(mcuH, imageH - aMCUY * mcuH), rgb, 0, mcuW);
	}


//	public static void transform(JPEG aJPEG, IDCT aIDCT, BufferedImage aImage)
//	{
//		ComponentInfo[] components = aJPEG.mSOFSegment.getComponents();
//		int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
//		int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
//		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();
//		int numVerMCU = aJPEG.mSOFSegment.getVerMCU();
//		int numComponents = components.length;
//
//		int mcuW = 8 * maxSamplingX;
//		int mcuH = 8 * maxSamplingY;
//
//		int[][][][] coefficients = aJPEG.mCoefficients;
//
//		int[] blockLookup = new int[12];
//
//		int cp = 0;
//		int cii = 0;
//		for (ComponentInfo ci : components)
//		{
//			for (int i = 0; i < ci.getVerSampleFactor() * ci.getHorSampleFactor(); i++, cp++)
//			{
//				blockLookup[cp] = cii;
//			}
//			cii++;
//		}
//
//		blockLookup = Arrays.copyOfRange(blockLookup, 0, cp);
//
//		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
//		{
//			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
//			{
//				for (int blockIndex = 0; blockIndex < blockLookup.length; blockIndex++)
//				{
//					ComponentInfo comp = components[blockLookup[blockIndex]];
//
//					QuantizationTable quantizationTable = aJPEG.mQuantizationTables[comp.getQuantizationTableId()];
//
//					aIDCT.transform(coefficients[mcuY][mcuX][blockIndex], quantizationTable);
//				}
//			}
//		}
//
//		int c0 = components[0].getComponentBlockOffset();
//		int c1 = components.length == 1 ? 0 : components[1].getComponentBlockOffset();
//		int c2 = components.length == 1 ? 0 : components[2].getComponentBlockOffset();
//
//		int h0 = components[0].getHorSampleFactor();
//		int v0 = components[0].getVerSampleFactor();
//		int h1 = components.length == 1 ? 0 : components[1].getHorSampleFactor();
//		int v1 = components.length == 1 ? 0 : components[1].getVerSampleFactor();
//		int h2 = components.length == 1 ? 0 : components[2].getHorSampleFactor();
//		int v2 = components.length == 1 ? 0 : components[2].getVerSampleFactor();
//
//		if (components.length == 1 && (h0 != 1 || v0 != 1))
//		{
//			throw new IllegalStateException("Unsupported subsampling");
//		}
//
//		for (int iy = 0; iy < aJPEG.mSOFSegment.getHeight(); iy++)
//		{
//			for (int ix = 0; ix < aJPEG.mSOFSegment.getWidth(); ix++)
//			{
//				int ixh0 = (ix % mcuW) * h0 / maxSamplingX;
//				int iyv0 = (iy % mcuH) * v0 / maxSamplingY;
//
//				int lu = coefficients[iy / mcuH][ix / mcuW][c0 + (ixh0 / 8) + h0 * (iyv0 / 8)][(ixh0 % 8) + 8 * (iyv0 % 8)];
//				int color;
//
//				if (numComponents == 1)
//				{
//					color = aJPEG.mColorSpace.yccToRgb(lu, 0, 0);
//				}
//				else if (numComponents == 3)
//				{
//					int cb;
//					int cr;
//
//					if (h0 == 2 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
//					{
//						cb = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_3X3);
//						cr = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_3X3);
//					}
//					else if (h0 == 1 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
//					{
//						cb = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_1X3);
//						cr = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_1X3);
//					}
//					else if (h0 == 2 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1)
//					{
//						cb = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_3X1);
//						cr = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_3X1);
//					}
//					else
//					{
//						cb = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h1, v1, maxSamplingX, maxSamplingY, coefficients, c1, KERNEL_1X1);
//						cr = upsampleChroma(aJPEG, ix, iy, mcuW, mcuH, h2, v2, maxSamplingX, maxSamplingY, coefficients, c2, KERNEL_1X1);
//					}
//
//					color = aJPEG.mColorSpace.yccToRgb(lu, cb, cr);
//				}
//				else
//				{
//					throw new IllegalStateException();
//				}
//
//				aImage.setRGB(ix, iy, color);
//			}
//		}
//	}


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


//	private static int upsampleChroma(int aMCUX, int aMCUY, int aBlockIndex, JPEG aJPEG, int ix, int iy, int aMcuW, int aMcuH, int aSamplingHor, int aSamplingVer, int aMaxSamplingX, int aMaxSamplingY, int[][][][] aCoefficients, int aComponentOffset, int[][] aKernel)
//	{
//		return aCoefficients[aBlockIndex][8*iy+ix];

//		int sum = 0;
//		int total = 0;
//
//		int x = ix - aKernel[0].length / 2;
//		int y = iy - aKernel.length / 2;
//		int iw = aJPEG.mSOFSegment.getWidth();
//		int ih = aJPEG.mSOFSegment.getHeight();
//
//		int fx = aKernel[0].length / 2;
//		int fy = aKernel.length / 2;
//
//		for (int fy = 0; fy < aKernel.length; fy++)
//		{
//			for (int fx = 0; fx < aKernel[0].length; fx++)
//			{
//				if (x + fx >= 0 && y + fy >= 0 && x + fx < iw && y + fy < ih)
//				{
//					int bx = ((x + fx) % aMcuW) * aSamplingHor / aMaxSamplingX;
//					int by = ((y + fy) % aMcuH) * aSamplingVer / aMaxSamplingY;
//
//					int w = aKernel[fy][fx];
//
//					sum += w * aCoefficients[(y + fy) / aMcuH][(x + fx) / aMcuW][aComponentOffset + (bx / 8) + aSamplingHor * (by / 8)][(bx % 8) + 8 * (by % 8)];
//
//					total += w;
//				}
//			}
//		}
//
//		return (int)Math.round(sum / (double)total);
//	}
}
