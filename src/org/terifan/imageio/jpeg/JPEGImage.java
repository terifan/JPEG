package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;


public class JPEGImage
{
	private BufferedImage mImage;


	public JPEGImage(int aWidth, int aHeight, int aMaxSamplingX, int aMaxSamplingY, int aComponents)
	{
		mImage = new BufferedImage(aWidth, aHeight, BufferedImage.TYPE_INT_RGB);
//		mRaster = ((DataBufferInt)mImage.getRaster().getDataBuffer()).getData();
	}


	public BufferedImage getImage()
	{
		return mImage;
	}
}
