package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class SOFSegment
{
	private int mPrecision;
	private int mHeight;
	private int mWidth;
	private ComponentInfo[] mComponents;
	private JPEG mJPEG;


	public SOFSegment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public SOFSegment(JPEG aJPEG, int aWidth, int aHeight, int aPrecision, ComponentInfo... aComponents)
	{
		mJPEG = aJPEG;
		mWidth = aWidth;
		mHeight = aHeight;
		mPrecision = aPrecision;
		mComponents = aComponents;
	}


	public SOFSegment read(BitInputStream aBitStream) throws IOException
	{
		int segmentLength = aBitStream.readInt16();

		if (segmentLength != 11 && segmentLength != 17)
		{
			throw new IOException("segmentLength illegal value: " + segmentLength);
		}

		mPrecision = aBitStream.readInt8();
		mHeight = aBitStream.readInt16();
		mWidth = aBitStream.readInt16();
		mComponents = new ComponentInfo[aBitStream.readInt8()];

		if (mPrecision != 8)
		{
			throw new IOException("mPrecision illegal value: " + mPrecision);
		}

		for (int i = 0; i < mComponents.length; i++)
		{
			mComponents[i] = new ComponentInfo().read(aBitStream, i);
		}

		if (VERBOSE)
		{
			System.out.println("SOFMarkerSegment[precision=" + mPrecision + "bits, width=" + mWidth + ", height=" + mHeight + ", numComponents=" + mComponents.length + "]");

			for (ComponentInfo mComponent : mComponents)
			{
				System.out.println("  " + mComponent);
			}
		}

		return this;
	}


	public SOFSegment write(BitOutputStream aBitStream) throws IOException
	{
		if (mJPEG.mArithmetic && mJPEG.mProgressive)
		{
			aBitStream.writeInt16(JPEGConstants.SOF10);
		}
		else if (mJPEG.mArithmetic)
		{
			aBitStream.writeInt16(JPEGConstants.SOF9);
		}
		else if (mJPEG.mProgressive)
		{
			aBitStream.writeInt16(JPEGConstants.SOF2);
		}
		else
		{
			aBitStream.writeInt16(JPEGConstants.SOF0);
		}

		aBitStream.writeInt16(2 + 6 + 3 * 3);

		aBitStream.writeInt8(mPrecision);
		aBitStream.writeInt16(mHeight);
		aBitStream.writeInt16(mWidth);
		aBitStream.writeInt8(mComponents.length);

		for (int i = 0; i < mComponents.length; i++)
		{
			mComponents[i].write(aBitStream);
		}

		if (VERBOSE)
		{
			System.out.println("SOFMarkerSegment[precision=" + mPrecision + "bits, width=" + mWidth + ", height=" + mHeight + ", numComponents=" + mComponents.length + "]");

			for (ComponentInfo mComponent : mComponents)
			{
				System.out.println("  " + mComponent);
			}
		}

		return this;
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


	public int getMaxHorSampling()
	{
		int maxSamplingX = 0;
		for (ComponentInfo ci : mComponents)
		{
			maxSamplingX = Math.max(maxSamplingX, ci.getHorSampleFactor());
		}
		return maxSamplingX;
	}


	public int getMaxVerSampling()
	{
		int maxSamplingY = 0;
		for (ComponentInfo ci : mComponents)
		{
			maxSamplingY = Math.max(maxSamplingY, ci.getVerSampleFactor());
		}
		return maxSamplingY;
	}


	public int getHorMCU()
	{
		int maxSamplingX = getMaxHorSampling();

		return (mWidth + 8 * maxSamplingX - 1) / (8 * maxSamplingX);
	}


	public int getVerMCU()
	{
		int maxSamplingY = getMaxVerSampling();

		return (mHeight + 8 * maxSamplingY - 1) / (8 * maxSamplingY);
	}
}