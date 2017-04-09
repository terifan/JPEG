package org.terifan.multimedia.jpeg;

import java.awt.Point;
import java.io.IOException;


class ComponentInfo
{
	public final static int Y = 1;
	public final static int Cb = 2;
	public final static int Cr = 3;
	public final static int I = 4;
	public final static int Q = 5;
	
	private int mComponent;
	private int mQuantizationTableId;
	private Point mSampling;


	public static ComponentInfo read(BitInputStream aInputStream) throws IOException
	{
		ComponentInfo ci = new ComponentInfo();

		ci.mComponent = aInputStream.readInt8();
		int temp = aInputStream.readInt8();
		ci.mSampling = new Point(temp >> 4, temp & 0x0f);
		ci.mQuantizationTableId = aInputStream.readInt8();

		if (ci.mComponent < 1 || ci.mComponent > 5)
		{
//			throw new IOException("Error in JPEG stream; Undefined component type: " + ci.mComponent);
		}

		return ci;
	}


	public int getComponent()
	{
		return mComponent;
	}


	public Point getSampling()
	{
		return mSampling;
	}


	public int getQuantizationTableId()
	{
		return mQuantizationTableId;
	}


	@Override
	public String toString()
	{
		String component;

		switch (mComponent)
		{
			case Y: component = "Y"; break;
			case Cb: component = "Cb"; break;
			case Cr: component = "Cr"; break;
			case I: component = "I"; break;
			default: component = "Q"; break;
		}

		return "ComponentInfo[component=" + component + ", sampling=[" + mSampling.x + "," + mSampling.y + "], quantizationTableId=" + mQuantizationTableId + "]";
	}
}