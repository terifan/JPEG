package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;


public class ColorSpaceRGBGrayscale implements ColorSpace
{
	private final static long serialVersionUID = 1L;


	@Override
	public void configureImageBuffer(SOFSegment aSOFSegment, JPEGImage aImage)
	{
		aImage.configure(aSOFSegment.getWidth(), aSOFSegment.getHeight(), BufferedImage.TYPE_INT_RGB);
	}


	@Override
	public void encode(int[] aRGB, int[] aLu, int[] aVoid1, int[] aVoid2)
	{
		for (int i = 0; i < aRGB.length; i++)
		{
			int c = aRGB[i];
			int R = 255 & (c >> 16);
			int G = 255 & (c >> 8);
			int B = 255 & (c);

			int lu = clamp(R * 0.29900 + G * 0.58700 + B * 0.11400);

			aLu[i] = lu;
			aVoid1[i] = 0;
			aVoid2[i] = 0;
		}
	}


	@Override
	public int decode(int aY, int aVoid1, int aVoid2)
	{
		return (aY << 16) + (aY << 8) + aY;
	}


	private static int clamp(double aValue)
	{
		return aValue < 0 ? 0 : aValue > 255 ? 255 : (int)(aValue+0.5);
	}


	@Override
	public String getName()
	{
		return "GrayScale";
	}
}
