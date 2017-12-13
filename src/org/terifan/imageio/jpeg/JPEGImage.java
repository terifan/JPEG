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


	private void copyBlock(int[] aSrc, int[] aDst, int aDstOffset)
	{
		for (int src = 0; src < 64; aDstOffset += mMCUWidth, src += 8)
		{
			System.arraycopy(aSrc, src, aDst, aDstOffset, 8);
		}
	}


	private void upsample8x16(int[] aSrc, int[] aDst, int aDstOffset, int aMcuWidth, int aMcuHeight, int aSamplingX, int aSamplingY)
	{
		try
		{
			for (int y = 0, dst = aDstOffset; y < 8; y++, dst+=mMCUWidth)
			{
				for (int x = 0, src = 0; x < 8; x++, dst++)
				{
					aDst[dst] = aDst[dst + mMCUWidth] = aSrc[src];
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("ERROR: " + aMcuWidth + " " + aMcuHeight + " " + aSamplingX + " " + aSamplingY);
			throw e;
		}
	}


	private void upsample16x16(int[] aCoefficients, int[] aBuffer, int aOffset, int aMcuWidth, int aMcuHeight, int aSamplingX, int aSamplingY)
	{
		try
		{
			int xShift = ((mMCUWidth / aSamplingX) >> 3) - 1;
			int yShift = ((mMCUHeight / aSamplingY) >> 3) - 1;

			for (int y = 0; y < aMcuHeight; y++, aOffset += aMcuWidth)
			{
				for (int x = 0, dst = aOffset, src = (y >> yShift) * 8; x < aMcuWidth; x++, dst++)
				{
					aBuffer[dst] = aCoefficients[(x >> xShift) + src];
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("ERROR: " + aMcuWidth + " " + aMcuHeight + " " + aSamplingX + " " + aSamplingY);
			throw e;
		}
	}


	public void setData(int aBlockX, int aBlockY, int aSamplingX, int aSamplingY, int[] aCoefficients, int[] aBuffer)
	{
		if (mMCUWidth == 8 && mMCUHeight == 8)
		{
			System.arraycopy(aCoefficients, 0, aBuffer, 0, 64);
		}
		else if (mMCUWidth == 8 * aSamplingX && mMCUHeight == 8 * aSamplingY)
		{
			copyBlock(aCoefficients, aBuffer, 8 * aBlockX + 8 * aBlockY * mMCUWidth);
		}
		else if (aSamplingX == 2)
		{
			upsample8x16(aCoefficients, aBuffer, aBlockX * 8 + aBlockY * 8 * mMCUWidth, mMCUWidth, mMCUHeight, aSamplingX, aSamplingY);
		}
		else
		{
			upsample16x16(aCoefficients, aBuffer, aBlockX * 8 + aBlockY * 8 * mMCUWidth, mMCUWidth, mMCUHeight, aSamplingX, aSamplingY);
		}
	}


	public void flushMCU(int aX, int aY, int[][] aBuffers)
	{
		aX *= mMCUWidth;
		aY *= mMCUHeight;

		int mcuWidth = Math.min(mMCUWidth, mWidth - aX);
		int mcuHeight = Math.min(mMCUHeight, mHeight - aY);

		int[] y = aBuffers[0];
		int[] cb = aBuffers[1];
		int[] cr = aBuffers[2];

		if (mComponents == 3)
		{
			for (int mcuY = 0; mcuY < mcuHeight; mcuY++)
			{
				for (int mcuX = 0, i = mcuY * mMCUWidth, dst = (aY + mcuY) * mWidth + aX; mcuX < mcuWidth; mcuX++, dst++, i++)
				{
					mRaster[dst] = ColorSpace.yuvToRgbFP(y, cb, cr, i);
				}
			}
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}
}
