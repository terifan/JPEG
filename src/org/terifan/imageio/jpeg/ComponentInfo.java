package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class ComponentInfo
{
	public final static int Y = 1;
	public final static int CB = 2;
	public final static int CR = 3;
	public final static int I = 4;
	public final static int Q = 5;

	private int mComponent;
	private int mQuantizationTableId;
	private int mSamplingX;
	private int mSamplingY;


	public static ComponentInfo read(BitInputStream aInputStream) throws IOException
	{
		ComponentInfo ci = new ComponentInfo();

		ci.mComponent = aInputStream.readInt8();
		int temp = aInputStream.readInt8();
		ci.mSamplingX = temp >> 4;
		ci.mSamplingY = temp & 0xf;
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


	public int getSamplingX()
	{
		return mSamplingX;
	}


	public int getSamplingY()
	{
		return mSamplingY;
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
			case CB: component = "Cb"; break;
			case CR: component = "Cr"; break;
			case I: component = "I"; break;
			default: component = "Q"; break;
		}

		return "ComponentInfo[component=" + component + ", sampling=[" + mSamplingX + "," + mSamplingY + "], quantizationTableId=" + mQuantizationTableId + "]";
	}
}