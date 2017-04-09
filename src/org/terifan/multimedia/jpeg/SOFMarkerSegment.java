package org.terifan.multimedia.jpeg;

import java.awt.Point;
import java.io.IOException;


class SOFMarkerSegment
{
	private int mPrecision;
	private int mHeight;
	private int mWidth;
	private ComponentInfo[] mComponents;
	private Point mMaxSampling;


	public SOFMarkerSegment(BitInputStream aInputStream) throws IOException
	{
		int segmentLength = aInputStream.readInt16();

		if (segmentLength != 11 && segmentLength != 17)
		{
			throw new IOException("segmentLength illegal value: " + segmentLength);
		}

		mPrecision = aInputStream.readInt8();
		mHeight = aInputStream.readInt16();
		mWidth = aInputStream.readInt16();
		mComponents = new ComponentInfo[aInputStream.readInt8()];

		if (mPrecision != 8)
		{
			throw new IOException("mPrecision illegal value: " + mPrecision);
		}

		mMaxSampling = new Point();

		for (int i = 0; i < mComponents.length; i++)
		{
			mComponents[i] = ComponentInfo.read(aInputStream);

			Point sampling = mComponents[i].getSampling();
			mMaxSampling.x = Math.max(mMaxSampling.x, sampling.x);
			mMaxSampling.y = Math.max(mMaxSampling.y, sampling.y);
		}

		if (JPEGImageReader.VERBOSE)
		{
			System.out.println("SOFMarkerSegment[precision=" + mPrecision + "bits, width=" + mWidth + ", height=" + mHeight + ", numComponents=" + mComponents.length + "]");

			for (ComponentInfo mComponent : mComponents)
			{
				System.out.println(mComponent);
			}
		}
	}


	public Point getMaxSampling()
	{
		return mMaxSampling;
	}


	public int getWidth()
	{
		return mWidth;
	}


	public int getHeight()
	{
		return mHeight;
	}


	public int getComponentCount()
	{
		return mComponents.length;
	}


	public ComponentInfo getComponent(int aIndex)
	{
		return mComponents[aIndex];
	}
}