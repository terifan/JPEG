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
	private CompressionType mCompressionType;
	private int[] mBlockLookup;


	public SOFSegment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public SOFSegment(JPEG aJPEG, CompressionType aCompressionType, int aWidth, int aHeight, int aPrecision, ComponentInfo... aComponents)
	{
		mJPEG = aJPEG;
		mCompressionType = aCompressionType;
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

		updateLookupTable();

		return this;
	}


	@Override
	public SOFSegment encode(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt16(mCompressionType.getSegmentMarker().CODE);
		aBitStream.writeInt16(2 + 6 + 3 * mComponents.length);
		aBitStream.writeInt8(mPrecision);
		aBitStream.writeInt16(mHeight);
		aBitStream.writeInt16(mWidth);
		aBitStream.writeInt8(mComponents.length);

		for (ComponentInfo comp : mComponents)
		{
			comp.encode(aBitStream);
		}

		updateLookupTable();

		return this;
	}


	@Override
	public SOFSegment print(Log aLog) throws IOException
	{
		aLog.println("SOF segment");
		aLog.println("  precision=%d bits, width=%d, height=%d", mPrecision, mWidth, mHeight);

		for (ComponentInfo mComponent : mComponents)
		{
			mComponent.print(aLog);
		}

		return this;
	}


	public CompressionType getCompressionType()
	{
		return mCompressionType;
	}


	public SOFSegment setCompressionType(CompressionType aCompressionType)
	{
		mCompressionType = aCompressionType;
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


	public ComponentInfo getComponentByBlockIndex(int aBlockIndex)
	{
		return mComponents[mBlockLookup[aBlockIndex]];
	}


	public int getBlocksInMCU()
	{
		return mBlockLookup.length;
	}


	private void updateLookupTable()
	{
		mBlockLookup = new int[12];

		int cp = 0;
		int cii = 0;
		for (ComponentInfo ci : mComponents)
		{
			for (int i = 0; i < ci.getVerSampleFactor() * ci.getHorSampleFactor(); i++, cp++)
			{
				mBlockLookup[cp] = cii;
			}
			cii++;
		}

		mBlockLookup = Arrays.copyOfRange(mBlockLookup, 0, cp);
	}


	// TODO: https://docs.oracle.com/javase/8/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html
	public String getColorSpaceName()
	{
//		if (mJPEG.mJFIFSegmentMarker != null)
//		{
			switch (mComponents.length)
			{
				case 1:
					return "grayscale";
				case 2:
					return "grayalpha";
				case 3:
					return "ycbcr";
				case 4:
					return "cmyk";
				default:
					throw new IllegalArgumentException();
			}
//		}
	}
}