package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;


public class SOFMarkerSegment
{
	private boolean mArithmetic;
	private boolean mProgressive;
	private int mPrecision;
	private int mHeight;
	private int mWidth;
	private ComponentInfo[] mComponents;
	private int mMaxSamplingX;
	private int mMaxSamplingY;


	public SOFMarkerSegment(BitInputStream aInputStream, boolean aArithmetic, boolean aProgressive) throws IOException
	{
		int segmentLength = aInputStream.readInt16();

		if (segmentLength != 11 && segmentLength != 17)
		{
			throw new IOException("segmentLength illegal value: " + segmentLength);
		}

		mArithmetic = aArithmetic;
		mProgressive = aProgressive;
		mPrecision = aInputStream.readInt8();
		mHeight = aInputStream.readInt16();
		mWidth = aInputStream.readInt16();
		mComponents = new ComponentInfo[aInputStream.readInt8()];

		if (mPrecision != 8)
		{
			throw new IOException("mPrecision illegal value: " + mPrecision);
		}

		for (int i = 0; i < mComponents.length; i++)
		{
			mComponents[i] = new ComponentInfo(aInputStream, i);
		}

		if (VERBOSE)
		{
			System.out.println("SOFMarkerSegment[precision=" + mPrecision + "bits, width=" + mWidth + ", height=" + mHeight + ", numComponents=" + mComponents.length + "]");

			for (ComponentInfo mComponent : mComponents)
			{
				System.out.println(mComponent);
			}
		}
	}


	public int getWidth()
	{
		return mWidth;
	}


	public int getHeight()
	{
		return mHeight;
	}


	public int getNumComponents()
	{
		return mComponents.length;
	}


	public ComponentInfo getComponent(int aIndex)
	{
		return mComponents[aIndex];
	}


	public boolean isArithmetic()
	{
		return mArithmetic;
	}


	public boolean isProgressive()
	{
		return mProgressive;
	}
}