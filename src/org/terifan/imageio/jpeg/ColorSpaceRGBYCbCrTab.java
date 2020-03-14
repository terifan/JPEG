package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;


public class ColorSpaceRGBYCbCrTab implements ColorSpace
{
	private final static int[] CR_R_TAB = new int[256];
	private final static int[] CB_B_TAB = new int[256];
	private final static int[] CR_G_TAB = new int[256];
	private final static int[] CB_G_TAB = new int[256];

	static
	{
		for (int i = 0, x = -128; i <= 255; i++, x++)
		{
			CR_R_TAB[i] = (int)( (1.40200 * 65536 + 0.5) * x + 32768) >> 16;
			CB_B_TAB[i] = (int)( (1.77200 * 65536 + 0.5) * x + 32768) >> 16;
			CR_G_TAB[i] = (int)(-(0.71414 * 65536 + 0.5)) * x;
			CB_G_TAB[i] = (int)(-(0.34414 * 65536 + 0.5)) * x + 32768;
		}
	}


	@Override
	public void configureImageBuffer(SOFSegment aSOFSegment, JPEGImage aImage)
	{
		aImage.configure(aSOFSegment.getWidth(), aSOFSegment.getHeight(), BufferedImage.TYPE_INT_RGB);
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

			aY[i]  = clamp( R * 0.29900 + G * 0.58700 + B * 0.11400);
			aCb[i] = clamp(-R * 0.16874 - G * 0.33126 + B * 0.50000 + 128);
			aCr[i] = clamp( R * 0.50000 - G * 0.41869 - B * 0.08131 + 128);
		}
	}


	@Override
	public int decode(int aY, int aCb, int aCr)
	{
		int y = aY;
		int cb = aCb;
		int cr = aCr;

		int r = clamp(y + CR_R_TAB[cr]);
		int g = clamp(y + ((CB_G_TAB[cb] + CR_G_TAB[cr]) >> 16));
		int b = clamp(y + CB_B_TAB[cb]);

		return (r << 16) + (g << 8) + b;
	}


	private static int clamp(int aValue)
	{
		return aValue < 0 ? 0 : aValue > 255 ? 255 : aValue;
	}


	private static int clamp(double aValue)
	{
		return aValue < 0 ? 0 : aValue > 255 ? 255 : (int)(aValue+0.5);
	}


	@Override
	public String getName()
	{
		return "YCbCr";
	}
}
