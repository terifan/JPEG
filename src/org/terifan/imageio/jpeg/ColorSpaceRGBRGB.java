package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;


public class ColorSpaceRGBRGB implements ColorSpace
{
	@Override
	public BufferedImage createBufferedImage(SOFSegment aSOFSegment)
	{
		return new BufferedImage(aSOFSegment.getWidth(), aSOFSegment.getHeight(), BufferedImage.TYPE_INT_RGB);
	}


	@Override
	public void encode(int[] aRGB, int[] aR, int[] aG, int[] aB)
	{
		for (int i = 0; i < aRGB.length; i++)
		{
			int c = aRGB[i];

			aR[i] = 0xff & (c >> 16);
			aG[i] = 0xff & (c >> 8);
			aB[i] = 0xff & (c);
		}
	}


	@Override
	public int decode(int aR, int aG, int aB)
	{
		return (aR << 16) + (aG << 8) + aB;
	}


	@Override
	public String getName()
	{
		return "RGB";
	}
}
