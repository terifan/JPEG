package org.terifan.imageio.jpeg;


public final class ColorSpace
{
//	JCS_UNKNOWN,		/* error/unspecified */
//	JCS_GRAYSCALE,		/* monochrome */
//	JCS_RGB,		/* red/green/blue, standard RGB (sRGB) */
//	JCS_YCbCr,		/* Y/Cb/Cr (also known as YUV), standard YCC */
//	JCS_CMYK,		/* C/M/Y/K */
//	JCS_YCCK,		/* Y/Cb/Cr/K */
//	JCS_BG_RGB,		/* big gamut red/green/blue, bg-sRGB */
//	JCS_BG_YCC		/* big gamut Y/Cb/Cr, bg-sYCC */

	private final static int FP_SCALEBITS = 16;
	private final static int FP_HALF = 1 << (FP_SCALEBITS - 1);
	private final static int FP_140200 = (int)(0.5 + (1 << FP_SCALEBITS) * 1.402);
	private final static int FP_034414 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.34414);
	private final static int FP_071414 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.71414);
	private final static int FP_177200 = (int)(0.5 + (1 << FP_SCALEBITS) * 1.772);

	private final static int[] CR_R_TAB = new int[256];
	private final static int[] CB_B_TAB = new int[256];
	private final static int[] CR_G_TAB = new int[256];
	private final static int[] CB_G_TAB = new int[256];

	public static enum ColorSpaceType
	{
		YCBCR,
		YCCK,
		RGB
	}

//	private final static int[] RANGE_LIMIT = new int[257 + 256 + 257];

	static
	{
//		for (int i = 0; i < 256; i++)
//		{
//			RANGE_LIMIT[257 + i] = i;
//		}
//		for (int i = 0; i < 257; i++)
//		{
//			RANGE_LIMIT[257 + 256 + i] = 255;
//		}

		for (int i = 0, x = -128; i <= 255; i++, x++)
		{
			CR_R_TAB[i] = (int)( (1.40200 * 65536 + 0.5) * x + 32768) >> 16;
			CB_B_TAB[i] = (int)( (1.77200 * 65536 + 0.5) * x + 32768) >> 16;
			CR_G_TAB[i] = (int)(-(0.71414 * 65536 + 0.5)) * x;
			CB_G_TAB[i] = (int)(-(0.34414 * 65536 + 0.5)) * x + 32768;
		}
	}


	public static int yuvToRgbLookup(int[] aY, int[] aCb, int[] aCr, int aOffset)
	{
		int y = aY[aOffset];
		int cb = aCb[aOffset];
		int cr = aCr[aOffset];

		int r = clamp(y + CR_R_TAB[cr]);
		int g = clamp(y + ((CB_G_TAB[cb] + CR_G_TAB[cr]) >> 16));
		int b = clamp(y + CB_B_TAB[cb]);

		return 0xff000000 | (r << 16) + (g << 8) + b;
	}


	public static int yuvToRgbFP(int[] aY, int[] aCb, int[] aCr, int aOffset)
	{
		int y = aY[aOffset];
		int cb = aCb[aOffset] - 128;
		int cr = aCr[aOffset] - 128;

		int r = clamp(y + ((FP_HALF +                  FP_140200 * cr) >> FP_SCALEBITS));
		int g = clamp(y - ((FP_HALF + FP_034414 * cb + FP_071414 * cr) >> FP_SCALEBITS));
		int b = clamp(y + ((FP_HALF + FP_177200 * cb                 ) >> FP_SCALEBITS));

		return 0xff000000 | (r << 16) + (g << 8) + b;
	}


	public static int yuvToRgbFP(int aY, int aCb, int aCr)
	{
		int y = aY;
		int cb = aCb - 128;
		int cr = aCr - 128;

		int r = clamp(y + ((FP_HALF +                  FP_140200 * cr) >> FP_SCALEBITS));
		int g = clamp(y - ((FP_HALF + FP_034414 * cb + FP_071414 * cr) >> FP_SCALEBITS));
		int b = clamp(y + ((FP_HALF + FP_177200 * cb                 ) >> FP_SCALEBITS));

		return 0xff000000 | (r << 16) + (g << 8) + b;
	}


	public static int yuvToRgbFloat(int[] aY, int[] aU, int[] aV, int aOffset)
	{
		int Y = aY[aOffset];
		int U = aU[aOffset];
		int V = aV[aOffset];

		int R = clamp(Y + 1.40200 * (V - 128));
		int G = clamp(Y - 0.34414 * (U - 128) - 0.71414 * (V - 128));
		int B = clamp(Y + 1.77200 * (U - 128));

		return (R << 16) + (G << 8) + B;
	}


	public static void rgbToYuvFloat(int[] aRGB, int[] aY, int[] aU, int[] aV)
	{
		for (int i = 0; i < aRGB.length; i++)
		{
			int c = aRGB[i];
			int R = 255 & (c >> 16);
			int G = 255 & (c >> 8);
			int B = 255 & (c);

			int Y = clamp(R * 0.29900 + G * 0.58700 + B * 0.11400);
			int U = clamp(R * -0.16874 + G * -0.33126 + B * 0.50000 + 128);
			int V = clamp(R * 0.50000 + G * -0.41869 + B * -0.08131 + 128);

			aY[i] = Y;
			aU[i] = U;
			aV[i] = V;
		}
	}


	private static int clamp(int aValue)
	{
//		return RANGE_LIMIT[257 + aValue];
		return aValue < 0 ? 0 : aValue > 255 ? 255 : aValue;
	}


	private static int clamp(double aValue)
	{
		return aValue < 0 ? 0 : aValue > 255 ? 255 : (int)(aValue+0.5);
	}


	private static int clamp2(int aValue)
	{
		return aValue < -255 ? -255 : aValue > 255 ? 255 : aValue;
	}


	private static int floorDiv2(int aValue)
	{
		return aValue >= 0 ? aValue / 2 : -((-aValue + 1) / 2);
	}


	private static int ceilDiv2(int aValue)
	{
		return aValue >= 0 ? (aValue + 1) / 2 : -((-aValue) / 2);
	}
}
