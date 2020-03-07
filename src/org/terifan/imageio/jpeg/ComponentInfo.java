package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class ComponentInfo
{
	public enum Type
	{
		Y, CB, CR, I, Q
	}

	public final static int Y = 1;
	public final static int CB = 2;
	public final static int CR = 3;
	public final static int I = 4;
	public final static int Q = 5;

	private int mComponentIndex; // identifier for this component (0..255)
	private int mComponentId; // its index in SOF or cinfo->comp_info[]
	private int mQuantizationTableId;
	private int mHorSampleFactor; // horizontal sampling factor (1..4)
	private int mVerSampleFactor; // vertical sampling factor (1..4)
	private int mTableDC; // DC entropy table selector (0..3)
	private int mTableAC; // AC entropy table selector (0..3)
	private int mComponentBlockOffset;


	public ComponentInfo()
	{
	}


	public ComponentInfo(int aComponentIndex, int aComponentId, int aQuantizationTableId, int aHorSampleFactor, int aVerSampleFactor)
	{
		mComponentIndex = aComponentIndex;
		mComponentId = aComponentId;
		mQuantizationTableId = aQuantizationTableId;
		mHorSampleFactor = aHorSampleFactor;
		mVerSampleFactor = aVerSampleFactor;
	}


	public ComponentInfo decode(BitInputStream aInputStream, int aComponentIndex) throws IOException
	{
		mComponentIndex = aComponentIndex;
		mComponentId = aInputStream.readInt8();
		mHorSampleFactor = aInputStream.readBits(4);
		mVerSampleFactor = aInputStream.readBits(4);
		mQuantizationTableId = aInputStream.readInt8();

		return this;
	}


	public ComponentInfo encode(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt8(mComponentId);
		aBitStream.writeBits(mHorSampleFactor, 4);
		aBitStream.writeBits(mVerSampleFactor, 4);
		aBitStream.writeInt8(mQuantizationTableId);

		return this;
	}


	public void print(Log aLog)
	{
		aLog.println("  component " + ComponentInfo.Type.values()[mComponentId - 1].name());
		aLog.println("    id=" + mComponentIndex + ", dc-table=" + mTableDC + ", ac-table=" + mTableAC + ", quantizationTableId=" + mQuantizationTableId + ", sample-factor=" + mHorSampleFactor + "x" + mVerSampleFactor);
	}


	public int getComponentIndex()
	{
		return mComponentIndex;
	}


	public int getComponentId()
	{
		return mComponentId;
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


	public void setComponentBlockOffset(int aComponentBlockOffset)
	{
		mComponentBlockOffset = aComponentBlockOffset;
	}


	public int getComponentBlockOffset()
	{
		return mComponentBlockOffset;
	}
}