package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;


public class JPEGImage
{
	private int mWidth;
	private int mHeight;
	private int mMCUWidth;
	private int mMCUHeight;
	private int mComponents;
	private int [] mRaster;
	private BufferedImage mImage;


	public JPEGImage(int aWidth, int aHeight, int aMaxSamplingX, int aMaxSamplingY, int aComponents)
	{
		mWidth = aWidth;
		mHeight = aHeight;
		mMCUWidth = 8 * aMaxSamplingX;
		mMCUHeight = 8 * aMaxSamplingY;
		mComponents = aComponents;
		mImage = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_INT_RGB);
		mRaster = ((DataBufferInt)mImage.getRaster().getDataBuffer()).getData();
	}


	public BufferedImage getImage()
	{
		return mImage;
	}


	public int[] getRaster()
	{
		return mRaster;
	}
}
