package org.terifan.imageio.jpeg;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;


public class JPEGImage
{
	private final int mWidth;
	private final int mHeight;
	private final int mMCUWidth;
	private final int mMCUHeight;
	private final int mComponents;
	private final Point mLastMCUPosition;
	private int [] mRaster;
	private boolean mIsDamaged;
	private boolean mDecodingErrors;
	private BufferedImage mImage;


	public JPEGImage(int aWidth, int aHeight, int aMaxSamplingX, int aMaxSamplingY, int aDensitiesUnits, int aDensityX, int aDensityY, int aComponents)
	{
		mWidth = aWidth;
		mHeight = aHeight;
		mMCUWidth = 8 * aMaxSamplingX;
		mMCUHeight = 8 * aMaxSamplingY;
		mLastMCUPosition = new Point();
		mComponents = aComponents;
		mImage = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_INT_RGB);
		mRaster = ((DataBufferInt)mImage.getRaster().getDataBuffer()).getData();
	}

	public boolean isDamaged()
	{
		return mIsDamaged;
	}


	public boolean isDecodingErrors()
	{
		return mDecodingErrors;
	}


	public BufferedImage getImage()
	{
		return mImage;
	}


	public void setDamaged()
	{
		mIsDamaged = true;

		int w = mLastMCUPosition.x + mMCUWidth;
		for (int y = mLastMCUPosition.y, yy = y * mWidth; y < mHeight; y++, yy += mWidth)
		{
			for (int x = w; x < mWidth; x++)
			{
				mRaster[x + yy] = (((x >> 3) & 1) ^ ((y >> 3) & 1)) == 0 ? 0xffe0e0e0 : 0xffffffff;
			}
		}
		for (int y = mLastMCUPosition.y + mMCUHeight, yy = y * mWidth; y < mHeight; y++, yy += mWidth)
		{
			for (int x = 0; x < w; x++)
			{
				mRaster[x + yy] = (((x >> 3) & 1) ^ ((y >> 3) & 1)) == 0 ? 0xffe0e0e0 : 0xffffffff;
			}
		}
	}


	private void copyBlock(int[] aCoefficients, int[] aBuffer, int aWidth, int aDst)
	{
		for (int src = 0; src < 64; aDst += aWidth, src += 8)
		{
			for (int i = 0; i < 64; i++)
			{
				aBuffer[i] += aCoefficients[i];
			}
//			System.arraycopy(aCoefficients, src, aBuffer, aDst, 8);
		}
	}


	private void scaleBlock(int[] aCoefficients, int[] aBuffer, int aOffset, int aMcuWidth, int aMcuHeight, int aSamplingX, int aSamplingY)
	{
		try
		{
			int xShift = ((mMCUWidth / aSamplingX) >> 3) - 1;
			int yShift = ((mMCUHeight / aSamplingY) >> 3) - 1;

			for (int y = 0; y < aMcuHeight; y++, aOffset += aMcuWidth)
			{
				for (int x = 0, dst = aOffset, src = (y >> yShift) * 8; x < aMcuWidth; x++, dst++)
				{
					aBuffer[dst] += aCoefficients[(x >> xShift) + src];
				}
			}
		}
		catch (Exception e)
		{
			System.out.println(aMcuWidth + " " + aMcuHeight + " " + aSamplingX + " " + aSamplingY);
			throw e;
		}
	}


	public void setData(int cx, int cy, int aSamplingX, int aSamplingY, int[] aBuffer, int[] aCoefficients)
	{
		if (mMCUWidth == 8 && mMCUHeight == 8)
		{
//			System.arraycopy(aCoefficients, 0, aBuffers, 0, 64);
			for (int i = 0; i < 64; i++)
			{
				aBuffer[i] += aCoefficients[i];
			}
		}
		else if (mMCUWidth == 8 * aSamplingX && mMCUHeight == 8 * aSamplingY)
		{
			copyBlock(aCoefficients, aBuffer, mMCUWidth, 8 * cx + 8 * cy * mMCUWidth);
		}
		else
		{
			scaleBlock(aCoefficients, aBuffer, cx * 8 + cy * 8 * mMCUWidth, mMCUWidth, mMCUHeight, aSamplingX, aSamplingY);
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
				for (int mcuX = 0, src = mcuY * mMCUWidth, dst = (aY + mcuY) * mWidth + aX; mcuX < mcuWidth; mcuX++, dst++, src++)
				{
					mRaster[dst] = ColorSpace.yuvToRgbFP(y, cb, cr, src);
				}
			}
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}
}
