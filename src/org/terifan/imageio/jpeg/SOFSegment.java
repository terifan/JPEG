package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class SOFSegment extends Segment implements Serializable
{
	private final static long serialVersionUID = 1L;

	private int mPrecision;
	private int mHeight;
	private int mWidth;
	private ComponentInfo[] mComponents;
	private CompressionType mCompressionType;
	private int[] mBlockLookup;
//	private int mBlockCount;


	public SOFSegment()
	{
	}


	public SOFSegment(CompressionType aCompressionType, int aWidth, int aHeight, int aPrecision, ComponentInfo... aComponents)
	{
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

		if (mComponents.length == 3 && mComponents[0].getComponentId() == 0 && mComponents[1].getComponentId() == 1 && mComponents[2].getComponentId() == 2)
		{
//			mJPEG.mAdjustComponentId = 1;

			for (int i = 0; i < mComponents.length; i++)
			{
				mComponents[i].setComponentId(mComponents[i].getComponentId() + 1);
			}
		}

		updateLookupTable();

		int count = 0;
		for (ComponentInfo ci : mComponents)
		{
			ci.setComponentBlockOffset(count);
			count += ci.getHorSampleFactor() * ci.getVerSampleFactor();
		}

		return this;
	}


	@Override
	public SOFSegment encode(JPEG aJPEG, BitOutputStream aBitStream) throws IOException
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
	public SOFSegment print(JPEG aJPEG, Log aLog) throws IOException
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
//					return "bg_ycc";
				case 4:
					return "cmyk";
				default:
					throw new IllegalArgumentException();
			}
//		}
	}


	public SubsamplingMode getSubsamplingMode()
	{
		int h0 = mComponents[0].getHorSampleFactor();
		int v0 = mComponents[0].getVerSampleFactor();
		int h1 = mComponents.length == 1 ? 0 : mComponents[1].getHorSampleFactor();
		int v1 = mComponents.length == 1 ? 0 : mComponents[1].getVerSampleFactor();
		int h2 = mComponents.length == 1 ? 0 : mComponents[2].getHorSampleFactor();
		int v2 = mComponents.length == 1 ? 0 : mComponents[2].getVerSampleFactor();

		if (h0 == 1 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:4:4
		{
			return SubsamplingMode._444;
		}
		if (h0 == 2 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:2:0
		{
			return SubsamplingMode._420;
		}
		if (h0 == 2 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:2:2 (hor)
		{
			return SubsamplingMode._422;
		}
		if (h0 == 1 && v0 == 2 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:2:2 (ver)
		{
			return SubsamplingMode._422;
		}
		if (h0 == 2 && v0 == 2 && h1 == 2 && v1 == 1 && h2 == 2 && v2 == 1) // 4:4:0
		{
			return SubsamplingMode._440;
		}
		if (h0 == 4 && v0 == 1 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:1:1 (hor)
		{
			return SubsamplingMode._411;
		}
		if (h0 == 1 && v0 == 4 && h1 == 1 && v1 == 1 && h2 == 1 && v2 == 1) // 4:1:1 (ver)
		{
//			return SubsamplingMode._411;
		}

		throw new IllegalArgumentException(h0+" "+v0+" "+h1+" "+v1+" "+h2+" "+v2);
	}
}