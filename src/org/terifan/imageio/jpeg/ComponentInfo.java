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
	private int mHorSampleFactor; // horizontal sampling factor (1..4)
	private int mVerSampleFactor; // vertical sampling factor (1..4)
	private int mTableDC; // DC entropy table selector (0..3)
	private int mTableAC; // AC entropy table selector (0..3)


	public ComponentInfo(BitInputStream aInputStream, int aComponentId) throws IOException
	{
		mComponentId = aComponentId;
		mComponentIndex = aInputStream.readInt8();
		mHorSampleFactor = aInputStream.readBits(4);
		mVerSampleFactor = aInputStream.readBits(4);
		mQuantizationTableId = aInputStream.readInt8();
	}


	public int getComponentId()
	{
		return mComponentId;
	}


	public int getComponentIndex()
	{
		return mComponentIndex;
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


	public int getTableDC()
	{
		return mTableDC;
	}


	public void setTableDC(int aSOSTableDC)
	{
		mTableDC = aSOSTableDC;
	}


	public int getTableAC()
	{
		return mTableAC;
	}


	public void setTableAC(int aSOSTableAC)
	{
		mTableAC = aSOSTableAC;
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

		return "component=" + component + ", dc-table=" + mTableDC + ", ac-table=" + mTableAC + ", quantizationTableId=" + mQuantizationTableId + ", sample-factor=" + mHorSampleFactor + "x" + mVerSampleFactor + ", id=" + mComponentId;
	}
}