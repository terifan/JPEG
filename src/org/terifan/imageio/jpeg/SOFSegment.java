package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.util.Arrays;
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
		mJPEG.num_components = aBitStream.readInt8();

		if (mPrecision != 8)
		{
			throw new IOException("mPrecision illegal value: " + mPrecision);
		}

		mComponents = new ComponentInfo[mJPEG.num_components];

		for (int i = 0; i < mJPEG.num_components; i++)
		{
			mComponents[i] = new ComponentInfo().read(aBitStream, i);
		}

		mJPEG.width = mWidth;
		mJPEG.height = mHeight;
		mJPEG.components = mComponents;
		mJPEG.precision = mPrecision;
		
		if (mJPEG.num_components == 1)
		{
			mJPEG.mColorSpace = ColorSpace.GRAYSCALE;
		}

		if (VERBOSE)
		{
			log();
		}

		return this;
	}


	public void log()
	{
		System.out.println("SOFMarkerSegment");
		System.out.println("  precision=" + mPrecision + " bits");
		System.out.println("  dimensions=" + mWidth + "x" + mHeight);
		System.out.println("  numComponents=" + mComponents.length);

		for (ComponentInfo mComponent : mComponents)
		{
			System.out.println("    " + mComponent);
		}
	}


	public SOFSegment write(BitOutputStream aBitStream) throws IOException
	{
		boolean baseline = true;
		int type;

		if (mJPEG.mArithmetic && mJPEG.mProgressive)
		{
			type = JPEGConstants.SOF10;
		}
		else if (mJPEG.mArithmetic)
		{
			type = JPEGConstants.SOF9;
		}
		else if (mJPEG.mProgressive)
		{
			type = JPEGConstants.SOF2;
		}
		else if (baseline)
		{
			type = JPEGConstants.SOF0;
		}
		else
		{
			type = JPEGConstants.SOF1;
		}

		aBitStream.writeInt16(type);
		aBitStream.writeInt16(2 + 6 + 3 * mComponents.length);
		aBitStream.writeInt8(mPrecision);
		aBitStream.writeInt16(mHeight);
		aBitStream.writeInt16(mWidth);
		aBitStream.writeInt8(mComponents.length);

		for (ComponentInfo comp : mComponents)
		{
			comp.write(aBitStream);
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