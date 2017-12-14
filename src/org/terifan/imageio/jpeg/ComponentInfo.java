package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


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
	private int mComponentBlockOffset;


	public ComponentInfo()
	{
	}


	public ComponentInfo(int aComponentId, int aComponentIndex, int aQuantizationTableId, int aHorSampleFactor, int aVerSampleFactor)
	{
		mComponentId = aComponentId;
		mComponentIndex = aComponentIndex;
		mQuantizationTableId = aQuantizationTableId;
		mHorSampleFactor = aHorSampleFactor;
		mVerSampleFactor = aVerSampleFactor;
	}


	public ComponentInfo read(BitInputStream aInputStream, int aComponentId) throws IOException
	{
		mComponentId = aComponentId;
		mComponentIndex = aInputStream.readInt8();
		mHorSampleFactor = aInputStream.readBits(4);
		mVerSampleFactor = aInputStream.readBits(4);
		mQuantizationTableId = aInputStream.readInt8();

		return this;
	}


	public ComponentInfo write(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt8(mComponentIndex);
		aBitStream.writeBits(mHorSampleFactor, 4);
		aBitStream.writeBits(mVerSampleFactor, 4);
		aBitStream.writeInt8(mQuantizationTableId);

		return this;
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


	public void setComponentBlockOffset(int aComponentBlockOffset)
	{
		mComponentBlockOffset = aComponentBlockOffset;
	}


	public int getComponentBlockOffset()
	{
		return mComponentBlockOffset;
	}
}