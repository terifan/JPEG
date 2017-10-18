package org.terifan.multimedia.jpeg;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;


public class JPEGImage
{
	private final static int FP_SCALEBITS = 16;
	private final static int FP_HALF = 1 << (FP_SCALEBITS - 1);
	private final static int FP_140200 = (int)(0.5 + (1 << FP_SCALEBITS) * 1.402);
	private final static int FP_034414 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.34414);
	private final static int FP_071414 = (int)(0.5 + (1 << FP_SCALEBITS) * 0.71414);
	private final static int FP_177200 = (int)(0.5 + (1 << FP_SCALEBITS) * 1.772);

	private final int mWidth;
	private final int mHeight;
	private final int[][] mBuffers;
	private final int mMCUWidth;
	private final int mMCUHeight;
	private final int mComponents;
	private final Point mLastMCUPosition;
	private int [] mRaster;
	private boolean mIsDamaged;
	private boolean mDecodingErrors;
	private BufferedImage mImage;


	JPEGImage(int aWidth, int aHeight, int aMaxSamplingX, int aMaxSamplingY, int aDensitiesUnits, int aDensityX, int aDensityY, int aComponents)
	{
		mWidth = aWidth;
		mHeight = aHeight;
		mBuffers = new int[JPEGImageReader.MAX_CHANNELS][aMaxSamplingX * aMaxSamplingY * 64];
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


	BufferedImage getImage()
	{
		return mImage;
	}


	void setDamaged()
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

	/*
	 1x1 1x1 1x1
	 2x1 1x1 1x1
	 1x2 1x1 1x1
	 2x2 1x1 1x1
	 2x2 2x1 2x1
	 4x2 1x1 1x1
	 2x4 1x1 1x1
	 4x1 1x1 1x1
	 1x4 1x1 1x1
	 4x1 2x1 2x1
	 1x4 1x2 1x2
	 4x4 2x2 2x2
	 */

	private void copyBlock(int[] dctCoefficients, int[] buffer, int w, int dst)
	{
		for (int src = 0; src < 64; dst += w, src += 8)
		{
			System.arraycopy(dctCoefficients, src, buffer, dst, 8);
		}
	}


	private void scaleBlock(int[] aCoefficients, int[] aBuffer, int q, int mcuWidth, int mcuHeight, int xShift, int yShift)
	{
		for (int y = 0; y < mcuHeight; y++, q += mcuWidth)
		{
			for (int x = 0, dst = q, src = (y >> yShift) * 8; x < mcuWidth; x++, dst++)
			{
				aBuffer[dst] = aCoefficients[(x >> xShift) + src];
			}
		}
	}


	void setData(int cx, int cy, int aSamplingX, int aSamplingY, int aComponent, int[] aCoefficients)
	{
		int[] buffer = mBuffers[aComponent];

		if (mMCUWidth == 8 && mMCUHeight == 8)
		{
			System.arraycopy(aCoefficients, 0, buffer, 0, 64);
		}
		else if (mMCUWidth == 8 * aSamplingX && mMCUHeight == 8 * aSamplingY)
		{
			copyBlock(aCoefficients, buffer, mMCUWidth, 8 * cx + 8 * cy * mMCUWidth);
		}
		else
		{
			int mcuWidth = mMCUWidth;
			int mcuHeight = mMCUHeight;
			int blockWidth = mMCUWidth / aSamplingX;
			int blockHeight = mMCUHeight / aSamplingY;
			int xShift = blockWidth == 8 ? 0 : blockWidth == 16 ? 1 : blockWidth == 32 ? 2 : 3;
			int yShift = blockHeight == 8 ? 0 : blockHeight == 16 ? 1 : blockHeight == 32 ? 2 : 3;

			scaleBlock(aCoefficients, buffer, (cx * 8) + (cy * 8) * mcuWidth, mcuWidth, mcuHeight, xShift, yShift);
		}
	}


	void flushMCU(int aX, int aY)
	{
		try
		{
			aX *= mMCUWidth;
			aY *= mMCUHeight;

			mLastMCUPosition.x = aX;
			mLastMCUPosition.y = aY;

			int[] yComponent = mBuffers[0];
			int[] cbComponent = mBuffers[1];
			int[] crComponent = mBuffers[2];

			int mcuWidth = Math.min(mMCUWidth, mWidth - aX);
			int mcuHeight = Math.min(mMCUHeight, mHeight - aY);

			if (mComponents != 1)
			{
				for (int mcuY = 0; mcuY < mcuHeight; mcuY++)
				{
					for (int mcuX = 0, src = mcuY * mMCUWidth, dst = (aY + mcuY) * mWidth + aX; mcuX < mcuWidth; mcuX++, dst++, src++)
					{
						int lu = yComponent[src];
						int cb = cbComponent[src] - 128;
						int cr = crComponent[src] - 128;

						int r = clamp(lu + ((FP_HALF +                  FP_140200 * cr) >> FP_SCALEBITS));
						int g = clamp(lu - ((FP_HALF + FP_034414 * cb + FP_071414 * cr) >> FP_SCALEBITS));
						int b = clamp(lu + ((FP_HALF + FP_177200 * cb                 ) >> FP_SCALEBITS));

						mRaster[dst] = 0xff000000 | (r << 16) + (g << 8) + b;
					}
				}
			}
			else
			{
				for (int mcuY = 0; mcuY < mcuHeight; mcuY++)
				{
					for (int mcuX = 0, src = mcuY * mMCUWidth, dst = (aY + mcuY) * mWidth + aX; mcuX < mcuWidth; mcuX++, dst++, src++)
					{
						int lu = clamp(yComponent[src]);

						mRaster[dst] = 0xff000000 | (lu << 16) + (lu << 8) + lu;
					}
				}
			}
		}
		catch (Exception e)
		{
			mDecodingErrors = true;
		}
	}


	private int clamp(int aValue)
	{
		return aValue < 0 ? 0 : aValue > 255 ? 255 : aValue;
	}

}