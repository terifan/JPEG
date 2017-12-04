package org.terifan.imageio.jpeg;


public class ColorSpace
{
	private final static int FP_SCALEBITS = 16;
	private final static int FP_HALF = 1 << (FP_SCALEBITS - 1);
	private final static int FP_140200 = (int)(0.5 + (1 << FP_SCALEBITS) * 1.402);
	private final static int FP_034414 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.34414);
	private final static int FP_071414 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.71414);
	private final static int FP_177200 = (int)(0.5 + (1 << FP_SCALEBITS) * 1.772);

//	private final static int[] CR_R_TAB = new int[256];
//	private final static int[] CB_B_TAB = new int[256];
//	private final static int[] CR_G_TAB = new int[256];
//	private final static int[] CB_G_TAB = new int[256];
//
//	private final static int[] RANGE_LIMIT = new int[257 + 256 + 257];


//	static
//	{
//		for (int i = 0; i < 256; i++)
//		{
//			RANGE_LIMIT[257 + i] = i;
//		}
//		for (int i = 0; i < 257; i++)
//		{
//			RANGE_LIMIT[257 + 256 + i] = 255;
//		}
//
//		for (int i = 0, x = -128; i <= 255; i++, x++)
//		{
//			CR_R_TAB[i] = (int)( (1.40200 * 65536 + 0.5) * x + 32768) >> 16;
//			CB_B_TAB[i] = (int)( (1.77200 * 65536 + 0.5) * x + 32768) >> 16;
//			CR_G_TAB[i] = (int)(-(0.71414 * 65536 + 0.5)) * x;
//			CB_G_TAB[i] = (int)(-(0.34414 * 65536 + 0.5)) * x + 32768;
//		}
//	}


	public static int yuvToRgb(int[] aY, int[] aCb, int[] aCr, int aComponents, int aOffset)
	{
		if (aComponents == 3)
		{
			int y = aY[aOffset];
			int cb = aCb[aOffset] - 128;
			int cr = aCr[aOffset] - 128;

			int r = clamp(y + ((FP_HALF +                  FP_140200 * cr) >> FP_SCALEBITS));
			int g = clamp(y - ((FP_HALF + FP_034414 * cb + FP_071414 * cr) >> FP_SCALEBITS));
			int b = clamp(y + ((FP_HALF + FP_177200 * cb                 ) >> FP_SCALEBITS));

//			int y = aY[aOffset];
//			int cb = aCb[aOffset];
//			int cr = aCr[aOffset];
//
//			int r = clamp(y + CR_R_TAB[cr]);
//			int g = clamp(y + ((CB_G_TAB[cb] + CR_G_TAB[cr]) >> 16));
//			int b = clamp(y + CB_B_TAB[cb]);

			return 0xff000000 | (r << 16) + (g << 8) + b;
		}

		int lu = clamp(aY[aOffset]);

		return 0xff000000 | (lu << 16) + (lu << 8) + lu;
	}


	private static int clamp(int aValue)
	{
//		return RANGE_LIMIT[257 + aValue];
		return aValue < 0 ? 0 : aValue > 255 ? 255 : aValue;
	}
}
