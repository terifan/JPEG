package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;


public class JPEGImage
{
	protected BufferedImage mImage;
	protected int[] mIntBuffer;
	protected byte[] mByteBuffer;
	private Runnable mRenderListener;


	void configure(int aWidth, int aHeight, int aType)
	{
		mImage = new BufferedImage(aWidth, aHeight, aType);

		if (aType == BufferedImage.TYPE_INT_RGB)
		{
			DataBufferInt buffer = (DataBufferInt)mImage.getRaster().getDataBuffer();
			mIntBuffer = buffer.getData();
		}
		else
		{
			DataBufferByte buffer = (DataBufferByte)mImage.getRaster().getDataBuffer();
			mByteBuffer = buffer.getData();
		}
	}


	void setRenderListener(Runnable aRenderListener)
	{
		mRenderListener = aRenderListener;
	}


	public void endOfScan(int aProgressionLevel)
	{
		if (mRenderListener != null)
		{
			mRenderListener.run();
		}
	}


	public BufferedImage getBufferedImage()
	{
		return mImage;
	}


	public int getWidth()
	{
		return mImage.getWidth();
	}


	public int getHeight()
	{
		return mImage.getHeight();
	}


	public int getRGB(int aX, int aY)
	{
		return mImage.getRGB(aX, aY);
	}


	public void setRGB(int aX, int aY, int aRGB)
	{
		mImage.setRGB(aX, aY, aRGB);
	}


	public void setRGB(int aX, int aY, int aW, int aH, int[] aRGB)
	{
		if (mIntBuffer != null)
		{
			for (int y = aY, h = Math.min(aY + aH, mImage.getHeight()), src = 0, dst = aY * mImage.getWidth() + aX, w = Math.min(aW, mImage.getWidth() - aX); y < h; y++, src+=aW, dst+=mImage.getWidth())
			{
				System.arraycopy(aRGB, src, mIntBuffer, dst, w);
			}
		}
		else
		{
			for (int y = aY, h = Math.min(aY + aH, mImage.getHeight()), src = 0, dst = aY * mImage.getWidth() + aX, w = Math.min(aW, mImage.getWidth() - aX); y < h; y++, src+=aW, dst+=mImage.getWidth())
			{
				for (int i = 0; i < w; i++)
				{
					mByteBuffer[dst + i] = (byte)aRGB[src + i];
				}
			}
		}
	}
}
