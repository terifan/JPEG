package org.terifan.imageio.jpeg;

import org.terifan.imageio.jpeg.decoder.IDCT;


public class ImageTransdecode
{
	public static void transform(JPEG aJPEG, IDCT aIDCT, JPEGImage aImage, int[][] aInput, int[] output)
	{
		SOFSegment sof = aJPEG.mSOFSegment;
		ComponentInfo[] components = sof.getComponents();

		int numComponents = components.length;

		int c0 = components[0].getComponentBlockOffset();
		int c1 = components.length == 1 ? 0 : components[1].getComponentBlockOffset();
		int c2 = components.length == 1 ? 0 : components[2].getComponentBlockOffset();

		int h0 = components[0].getHorSampleFactor();
		int v0 = components[0].getVerSampleFactor();
		int h1 = components.length == 1 ? 0 : components[1].getHorSampleFactor();
		int v1 = components.length == 1 ? 0 : components[1].getVerSampleFactor();
		int h2 = components.length == 1 ? 0 : components[2].getHorSampleFactor();
		int v2 = components.length == 1 ? 0 : components[2].getVerSampleFactor();

		if (numComponents == 1)
		{
		}
		else if (numComponents == 3)
		{
		}

		for (int blockIndex = 0; blockIndex < sof.getBlocksInMCU(); blockIndex++)
		{
			QuantizationTable quantizationTable = aJPEG.mQuantizationTables[sof.getComponentByBlockIndex(blockIndex).getQuantizationTableId()];

			aIDCT.transform(aInput[blockIndex], quantizationTable);
		}

		ColorSpace colorSpace = aJPEG.getColorSpace();

		if (h0 == 1 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:4:4
		{
			upsample444(aInput, c0, c1, c2, output, colorSpace);
		}
		else if (h0 == 2 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:2:0
		{
			upsample420(aInput, c0, c1, c2, output, colorSpace);
		}
		else if (h0 == 2 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:2:2 (hor)
		{
			upsample422(aInput, c0, c1, c2, output, colorSpace);
		}
//		else if (h0 == 1 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:2:2 (ver)
//		{
//			upsample422(aInput, c0, c1, c2, output, colorSpace);
//		}
		else if (h0 == 2 && v0 == 2 && h1 == 2 && v1 == 1 && h2 == 2 && v2 == 1) // 4:4:0
		{
			upsample440(aInput, c0, c1, c2, output, colorSpace);
		}
		else if (h0 == 4 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:1:1 (hor)
		{
			upsample411(aInput, c0, c1, c2, output, colorSpace);
		}
//		else if (h0 == 1 && v0 == 4 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:1:1 (ver)
//		{
//			upsample411(aInput, c0, c1, c2, output, colorSpace);
//		}
		else
		{
			throw new IllegalStateException("Unsupported subsampling");
		}
	}


	private static void upsample444(int[][] aWorkBlock, int aC0, int aC1, int aC2, int[] aOutput, ColorSpace aColorSpace)
	{
		for (int iy = 0, i = 0; iy < 8; iy++)
		{
			for (int ix = 0; ix < 8; ix++, i++)
			{
				int lu = aWorkBlock[aC0][i];
				int cb = aWorkBlock[aC1][i];
				int cr = aWorkBlock[aC2][i];
				aOutput[i] = aColorSpace.decode(lu, cb, cr);
			}
		}
	}


	private static void upsample420(int[][] aWorkBlock, int aC0, int aC1, int aC2, int[] aOutput, ColorSpace aColorSpace)
	{
		for (int by = 0; by < 2; by++)
		{
			for (int bx = 0; bx < 2; bx++)
			{
				for (int iy = 0; iy < 4; iy++)
				{
					for (int ix = 0; ix < 4; ix++)
					{
						int lu0 = aWorkBlock[aC0 + by * 2 + bx][iy * 2 * 8 + 0 + 2 * ix + 0];
						int lu1 = aWorkBlock[aC0 + by * 2 + bx][iy * 2 * 8 + 0 + 2 * ix + 1];
						int lu2 = aWorkBlock[aC0 + by * 2 + bx][iy * 2 * 8 + 8 + 2 * ix + 0];
						int lu3 = aWorkBlock[aC0 + by * 2 + bx][iy * 2 * 8 + 8 + 2 * ix + 1];
						int cb = aWorkBlock[aC1][by * 4 * 8 + bx * 4 + iy * 8 + ix];
						int cr = aWorkBlock[aC2][by * 4 * 8 + bx * 4 + iy * 8 + ix];
						aOutput[by * 8 * 16 + bx * 8 + (2 * iy + 0) * 16 + ix * 2 + 0] = aColorSpace.decode(lu0, cb, cr);
						aOutput[by * 8 * 16 + bx * 8 + (2 * iy + 0) * 16 + ix * 2 + 1] = aColorSpace.decode(lu1, cb, cr);
						aOutput[by * 8 * 16 + bx * 8 + (2 * iy + 1) * 16 + ix * 2 + 0] = aColorSpace.decode(lu2, cb, cr);
						aOutput[by * 8 * 16 + bx * 8 + (2 * iy + 1) * 16 + ix * 2 + 1] = aColorSpace.decode(lu3, cb, cr);
					}
				}
			}
		}
	}


	private static void upsample422(int[][] aWorkBlock, int aC0, int aC1, int aC2, int[] aOutput, ColorSpace aColorSpace)
	{
		for (int bx = 0; bx < 2; bx++)
		{
			for (int iy = 0; iy < 8; iy++)
			{
				for (int ix = 0; ix < 4; ix++)
				{
					int lu0 = aWorkBlock[aC0 + bx][iy * 8 + 2 * ix + 0];
					int lu1 = aWorkBlock[aC0 + bx][iy * 8 + 2 * ix + 1];
					int cb = aWorkBlock[aC1][iy * 8 + 4 * bx + ix];
					int cr = aWorkBlock[aC2][iy * 8 + 4 * bx + ix];
					aOutput[bx * 8 + iy * 16 + ix * 2 + 0] = aColorSpace.decode(lu0, cb, cr);
					aOutput[bx * 8 + iy * 16 + ix * 2 + 1] = aColorSpace.decode(lu1, cb, cr);
				}
			}
		}
	}


	private static void upsample440(int[][] aWorkBlock, int aC0, int aC1, int aC2, int[] aOutput, ColorSpace aColorSpace)
	{
		for (int bx = 0; bx < 2; bx++)
		{
			for (int by = 0; by < 2; by++)
			{
				for (int iy = 0; iy < 4; iy++)
				{
					for (int ix = 0; ix < 8; ix++)
					{
						int lu0 = aWorkBlock[aC0 + by * 2 + bx][iy * 2 * 8 + 0 + ix];
						int lu1 = aWorkBlock[aC0 + by * 2 + bx][iy * 2 * 8 + 8 + ix];
						int cb = aWorkBlock[aC1 + bx][by * 4 * 8 + iy * 8 + ix];
						int cr = aWorkBlock[aC2 + bx][by * 4 * 8 + iy * 8 + ix];
						aOutput[by * 8 * 16 + (2 * iy + 0) * 16 + 8 * bx + ix] = aColorSpace.decode(lu0, cb, cr);
						aOutput[by * 8 * 16 + (2 * iy + 1) * 16 + 8 * bx + ix] = aColorSpace.decode(lu1, cb, cr);
					}
				}
			}
		}
	}


	private static void upsample411(int[][] aWorkBlock, int aC0, int aC1, int aC2, int[] aOutput, ColorSpace aColorSpace)
	{
		for (int bx = 0; bx < 4; bx++)
		{
			for (int iy = 0; iy < 8; iy++)
			{
				for (int ix = 0; ix < 2; ix++)
				{
					int lu0 = aWorkBlock[aC0 + bx][iy * 8 + 4 * ix + 0];
					int lu1 = aWorkBlock[aC0 + bx][iy * 8 + 4 * ix + 1];
					int lu2 = aWorkBlock[aC0 + bx][iy * 8 + 4 * ix + 2];
					int lu3 = aWorkBlock[aC0 + bx][iy * 8 + 4 * ix + 3];
					int cb = aWorkBlock[aC1][iy * 8 + bx * 2 + ix];
					int cr = aWorkBlock[aC2][iy * 8 + bx * 2 + ix];
					aOutput[32 * iy + 8 * bx + 4 * ix + 0] = aColorSpace.decode(lu0, cb, cr);
					aOutput[32 * iy + 8 * bx + 4 * ix + 1] = aColorSpace.decode(lu1, cb, cr);
					aOutput[32 * iy + 8 * bx + 4 * ix + 2] = aColorSpace.decode(lu2, cb, cr);
					aOutput[32 * iy + 8 * bx + 4 * ix + 3] = aColorSpace.decode(lu3, cb, cr);
				}
			}
		}
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
//
//
//	private final static int[][] KERNEL_1X1 =
//	{
//		{1}
//	};
//
//
//	private final static int[][] KERNEL_3X3 =
//	{
//		{1,2,1},
//		{2,4,2},
//		{1,2,1}
//	};
//
//
//	private final static int[][] KERNEL_1X3 =
//	{
//		{1},
//		{2},
//		{1}
//	};
//
//
//	private final static int[][] KERNEL_3X1 =
//	{
//		{1,2,1}
//	};
//
//
//	private static int upsampleChroma(int aMCUX, int aMCUY, int aBlockIndex, JPEG aJPEG, int ix, int iy, int aMcuW, int aMcuH, int aSamplingHor, int aSamplingVer, int aMaxSamplingX, int aMaxSamplingY, int[][][][] aCoefficients, int aComponentOffset, int[][] aKernel)
//	{
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
