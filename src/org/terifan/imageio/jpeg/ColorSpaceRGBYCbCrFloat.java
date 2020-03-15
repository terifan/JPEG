package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;


public class ColorSpaceRGBYCbCrFloat implements ColorSpace
{
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
		int cb = aCb - 128;
		int cr = aCr - 128;

		int R = clamp(aY                 + 1.402000 * cr);
		int G = clamp(aY - 0.344136 * cb - 0.714136 * cr);
		int B = clamp(aY + 1.772000 * cb);

		return (R << 16) + (G << 8) + B;
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
