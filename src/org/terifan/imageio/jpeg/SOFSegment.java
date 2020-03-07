package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.util.Arrays;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class SOFSegment extends Segment
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


	@Override
	public SOFSegment decode(BitInputStream aBitStream) throws IOException
	{
		int segmentLength = aBitStream.readInt16();

		if (segmentLength != 11 && segmentLength != 17)
		{
			throw new IOException("segmentLength illegal value: " + segmentLength);
		}

		mPrecision = aBitStream.readInt8();
		mHeight = aBitStream.readInt16();
		mWidth = aBitStream.readInt16();
		int numComponents = aBitStream.readInt8();

		if (mPrecision != 8)
		{
			throw new IOException("mPrecision illegal value: " + mPrecision);
		}

		mComponents = new ComponentInfo[numComponents];

		for (int i = 0; i < numComponents; i++)
		{
			mComponents[i] = new ComponentInfo().decode(aBitStream, i);
		}

		if (numComponents == 1)
		{
			mJPEG.mColorSpace = ColorSpace.GRAYSCALE;
		}

		return this;
	}


	@Override
	public SOFSegment encode(BitOutputStream aBitStream) throws IOException
	{
		SegmentMarker type;

		if (mJPEG.mArithmetic && mJPEG.mProgressive)
		{
			type = SegmentMarker.SOF10;
		}
		else if (mJPEG.mArithmetic)
		{
			type = SegmentMarker.SOF9;
		}
		else if (mJPEG.mProgressive)
		{
			type = SegmentMarker.SOF2;
		}
		else if (false) // unsupported: extended huffman
		{
			type = SegmentMarker.SOF1;
		}
		else // baseline
		{
			type = SegmentMarker.SOF0;
		}

		aBitStream.writeInt16(type.CODE);
		aBitStream.writeInt16(2 + 6 + 3 * mComponents.length);
		aBitStream.writeInt8(mPrecision);
		aBitStream.writeInt16(mHeight);
		aBitStream.writeInt16(mWidth);
		aBitStream.writeInt8(mComponents.length);

		for (ComponentInfo comp : mComponents)
		{
			comp.encode(aBitStream);
		}

		return this;
	}


	@Override
	public SOFSegment print(Log aLog) throws IOException
	{
		aLog.println("SOF segment");
		aLog.println("  precision=" + mPrecision + " bits, width=" + mWidth + ", height=" + mHeight);

		for (ComponentInfo mComponent : mComponents)
		{
			mComponent.print(aLog);
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


	public ComponentInfo getComponentById(int aComponentId)
	{
		for (ComponentInfo ci : mComponents)
		{
			if (ci.getComponentId() == aComponentId)
			{
				return ci;
			}
		}

		throw new IllegalStateException("Component with ID " + aComponentId + " not found: "+Arrays.asList(mComponents)+"");
	}


	public int[] getComponentIds()
	{
		int[] list = new int[mComponents.length];
		for (int i = 0; i < list.length; i++)
		{
			list[i] = mComponents[i].getComponentId();
		}
		return list;
	}


	public ComponentInfo[] getComponents()
	{
		return mComponents;
	}


	public int getMaxBlocksInMCU()
	{
		int count = 0;

		for (ComponentInfo ci : mComponents)
		{
			count += ci.getHorSampleFactor() * ci.getVerSampleFactor();
		}

		return count;
	}
}