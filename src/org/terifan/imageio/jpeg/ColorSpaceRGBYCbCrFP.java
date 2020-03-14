package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;



public class ColorSpaceRGBYCbCrFP implements ColorSpace
{
	private final static int FP_SCALEBITS = 16;
	private final static int FP_HALF = 1 << (FP_SCALEBITS - 1);
	private final static int FP_140200 = (int)(0.5 + (1 << FP_SCALEBITS) * 1.402000);
	private final static int FP_034414 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.344136);
	private final static int FP_071414 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.714136);
	private final static int FP_177200 = (int)(0.5 + (1 << FP_SCALEBITS) * 1.772000);
	private final static int FP_029900 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.29900);
	private final static int FP_058700 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.58700);
	private final static int FP_011400 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.11400);
	private final static int FP_016874 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.16874);
	private final static int FP_033126 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.33126);
	private final static int FP_050000 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.50000);
	private final static int FP_041869 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.41869);
	private final static int FP_008131 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.08131);


	@Override
	public BufferedImage createBufferedImage(SOFSegment aSOFSegment)
	{
		return new BufferedImage(aSOFSegment.getWidth(), aSOFSegment.getHeight(), BufferedImage.TYPE_INT_RGB);
	}


	@Override
	public void encode(int[] aRGB, int[] aY, int[] aCb, int[] aCr)
	{
		for (int i = 0; i < aRGB.length; i++)
		{
			int c = aRGB[i];
			int R = 255 & (c >> 16);
			int G = 255 & (c >>  8);
			int B = 255 & (c      );

			aY[i]  = clamp((( R * FP_029900 + G * FP_058700 + B * FP_011400) >> FP_SCALEBITS)      );
			aCb[i] = clamp(((-R * FP_016874 - G * FP_033126 + B * FP_050000) >> FP_SCALEBITS) + 128);
			aCr[i] = clamp((( R * FP_050000 - G * FP_041869 - B * FP_008131) >> FP_SCALEBITS) + 128);
		}
	}


	@Override
	public int decode(int aY, int aCb, int aCr)
	{
		aCb -= 128;
		aCr -= 128;

		int r = clamp(aY + ((FP_HALF +                   FP_140200 * aCr) >> FP_SCALEBITS));
		int g = clamp(aY - ((FP_HALF + FP_034414 * aCb + FP_071414 * aCr) >> FP_SCALEBITS));
		int b = clamp(aY + ((FP_HALF + FP_177200 * aCb                  ) >> FP_SCALEBITS));

		return (r << 16) + (g << 8) + b;
	}


	private static int clamp(int aValue)
	{
		return aValue < 0 ? 0 : aValue > 255 ? 255 : aValue;
	}


	@Override
	public String getName()
	{
		return "YCbCr";
	}
}
