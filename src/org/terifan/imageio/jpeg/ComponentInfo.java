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

	private int mComponentId; // identifier for this component (0..255)
	private int mComponentIndex; // its index in SOF or cinfo->comp_info[]
	private int mQuantizationTableId;
	private int mTableDC; // DC entropy table selector (0..3)
	private int mTableAC; // AC entropy table selector (0..3)
	private int mHorSampleFactor; // horizontal sampling factor (1..4)
	private int mVerSampleFactor; // vertical sampling factor (1..4)


	public ComponentInfo(BitInputStream aInputStream) throws IOException
	{
		mComponentIndex = aInputStream.readInt8();
		mTableDC = aInputStream.readBits(4);
		mTableAC = aInputStream.readBits(4);
		mQuantizationTableId = aInputStream.readInt8();

		switch (mComponentIndex)
		{
			case Y: 
				mHorSampleFactor = 2;
				mVerSampleFactor = 2;
				break;
			case CB: 
			case CR: 
				mHorSampleFactor = 1;
				mVerSampleFactor = 1;
				break;
			default:
				throw new UnsupportedOperationException("component unsupported " + mComponentIndex);
		}
	}


	public int getComponentIndex()
	{
		return mComponentIndex;
	}


	public int getTableDC()
	{
		return mTableDC;
	}


	public int getTableAC()
	{
		return mTableAC;
	}


	public int getQuantizationTableId()
	{
		return mQuantizationTableId;
	}


	public int getHorSampleFactor()
	{
		return mHorSampleFactor;
	}


	public int getVerSampleFactor()
	{
		return mVerSampleFactor;
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

		return "component=" + component + ", dc-table=" + mTableDC + ", ac-table=" + mTableAC + ", quantizationTableId=" + mQuantizationTableId + ", sample-factor=" + mHorSampleFactor + "x" + mVerSampleFactor;
	}
}