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
						int co = by * 4 * 8 + iy * 8 + 4 * bx + ix;
						int cb = aWorkBlock[aC1][co];
						int cr = aWorkBlock[aC2][co];
						aOutput[by * 8 * 16 + bx * 8 + iy * 2 * 16 +  0 + ix * 2 + 0] = aColorSpace.decode(lu0, cb, cr);
						aOutput[by * 8 * 16 + bx * 8 + iy * 2 * 16 +  0 + ix * 2 + 1] = aColorSpace.decode(lu1, cb, cr);
						aOutput[by * 8 * 16 + bx * 8 + iy * 2 * 16 + 16 + ix * 2 + 0] = aColorSpace.decode(lu2, cb, cr);
						aOutput[by * 8 * 16 + bx * 8 + iy * 2 * 16 + 16 + ix * 2 + 1] = aColorSpace.decode(lu3, cb, cr);
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
}
