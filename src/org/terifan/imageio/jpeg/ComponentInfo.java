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

	private int mComponentIndex;
	private int mQuantizationTableId;
	private int mDCTableNo;
	private int mACTableNo;


	public ComponentInfo(BitInputStream aInputStream) throws IOException
	{
		mComponentIndex = aInputStream.readInt8();
		mDCTableNo = aInputStream.readBits(4);
		mACTableNo = aInputStream.readBits(4);
		mQuantizationTableId = aInputStream.readInt8();

//		if (mComponentIndex < 1 || mComponentIndex > 5)
//		{
//			throw new IOException("Error in JPEG stream; Undefined component type: " + ci.mComponentId);
//		}
	}


	public int getComponentIndex()
	{
		return mComponentIndex;
	}


	public int getDCTableNo()
	{
		return mDCTableNo;
	}


	public int getACTableNo()
	{
		return mACTableNo;
	}


	public int getQuantizationTableId()
	{
		return mQuantizationTableId;
	}


	@Override
	public String toString()
	{
		String component;

		switch (mComponentIndex)
		{
			case Y: component = "Y"; break;
			case CB: component = "Cb"; break;
			case CR: component = "Cr"; break;
			case I: component = "I"; break;
			default: component = "Q"; break;
		}

		return "ComponentInfo[component=" + component + ", sampling=[" + mDCTableNo + "," + mACTableNo + "], quantizationTableId=" + mQuantizationTableId + "]";
	}
}