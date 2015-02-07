package org.terifan.multimedia.jpeg;

import java.io.IOException;
import org.terifan.util.log.Log;


class SOFMarkerSegment
{
	private int mPrecision;
	private int mHeight;
	private int mWidth;
	private ComponentInfo[] mComponents;
	private int[] mMaxSampling;


	public SOFMarkerSegment(BitInputStream aInputStream) throws IOException
	{
		int segmentLength = aInputStream.readShort();

		if (segmentLength != 11 && segmentLength != 17)
		{
			throw new IOException("segmentLength illegal value: " + segmentLength);
		}

		mPrecision = aInputStream.readByte();
		mHeight = aInputStream.readShort();
		mWidth = aInputStream.readShort();
		mComponents = new ComponentInfo[aInputStream.readByte()];

		if (mPrecision != 8)
		{
			throw new IOException("mPrecision illegal value: " + mPrecision);
		}

		mMaxSampling = new int[2];

		for (int i = 0; i < mComponents.length; i++)
		{
			mComponents[i] = ComponentInfo.read(aInputStream);

			mMaxSampling[0] = Math.max(mMaxSampling[0], mComponents[i].getSampling()[0]);
			mMaxSampling[1] = Math.max(mMaxSampling[1], mComponents[i].getSampling()[1]);
		}

		if (JPEGImageReader.VERBOSE)
		{
			System.out.println("SOFMarkerSegment[precision=" + mPrecision + "bits, width=" + mWidth + ", height=" + mHeight + ", numComponents=" + mComponents.length + "]");

			for (int i = 0; i < mComponents.length; i++)
			{
				mComponents[i].debug();
			}
		}
	}


	public int[] getMaxSampling()
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


	static class ComponentInfo
	{
		public final static int Y = 1;
		public final static int Cb = 2;
		public final static int Cr = 3;
		public final static int I = 4;
		public final static int Q = 5;
		private int mComponent;
		private int mQuantizationTableId;
		private int[] mSampling;


		public static ComponentInfo read(BitInputStream aInputStream) throws IOException
		{
			ComponentInfo ci = new ComponentInfo();

			ci.mComponent = aInputStream.readByte();
			int temp = aInputStream.readByte();
			ci.mSampling = new int[]{temp >> 4, temp & 0x0f};
			ci.mQuantizationTableId = aInputStream.readByte();

			if (ci.mComponent < 1 || ci.mComponent > 5)
			{
//				throw new IOException("Error in JPEG stream; Undefined component type: " + ci.mComponent);
			}

			return ci;
		}


		public int getComponent()
		{
			return mComponent;
		}


		public int[] getSampling()
		{
			return mSampling;
		}


		public int getQuantizationTableId()
		{
			return mQuantizationTableId;
		}

		
		public void debug()
		{
			String component;

			switch (mComponent)
			{
				case Y: component = "Y"; break;
				case Cb: component = "Cb"; break;
				case Cr: component = "Cr"; break;
				case I: component = "I"; break;
				default: component = "Q";
			}

			Log.out.println("  ComponentInfo[component="+component+", sampling=["+mSampling[0]+","+mSampling[1]+"], quantizationTableId="+mQuantizationTableId+"]");
		}
	}
}