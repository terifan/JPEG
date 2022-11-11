package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.Serializable;
import org.terifan.imageio.jpeg.decoder.JPEGBitInputStream;
import org.terifan.imageio.jpeg.encoder.JPEGBitOutputStream;


public class ComponentInfo implements Serializable
{
	private final static long serialVersionUID = 1L;

	public enum Type
	{
		Y, CB, CR, I, Q;


//		static Type fromComponentId(int aComponentId)
//		{
//			return values()[aComponentId - 1];
//		}
	}

	private int mComponentIndex; // identifier for this component (0..255)
	private int mComponentId; // its index in SOF or cinfo->comp_info[]
	private int mQuantizationTableId;
	private int mHorSampleFactor; // horizontal sampling factor (1..4)
	private int mVerSampleFactor; // vertical sampling factor (1..4)
	private int mTableDC; // DC entropy table selector (0..3)
	private int mTableAC; // AC entropy table selector (0..3)
	private int mComponentBlockOffset;

	int componentIdX; // its index in SOF or cinfo->comp_info[]


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

		componentIdX = mComponentId;
	}


	public ComponentInfo decode(JPEGBitInputStream aInputStream, int aComponentIndex) throws IOException
	{
		mComponentIndex = aComponentIndex;
		mComponentId = aInputStream.readInt8();
		mHorSampleFactor = aInputStream.readBits(4);
		mVerSampleFactor = aInputStream.readBits(4);
		mQuantizationTableId = aInputStream.readInt8();

		componentIdX = mComponentId;

		return this;
	}


	public ComponentInfo encode(JPEGBitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt8(mComponentId);
		aBitStream.writeInt8((mHorSampleFactor << 4) + mVerSampleFactor);
		aBitStream.writeInt8(mQuantizationTableId);

		return this;
	}


	public void print(Log aLog)
	{
		aLog.println("  component %s", mComponentId);
//		aLog.println("  component %s", ComponentInfo.Type.fromComponentId(mComponentId));
		aLog.println("    id=%d, dc-table=%d, ac-table=%d, quantizationTableId=%d, sample-factor=%dx%d", mComponentIndex, mTableDC, mTableAC, mQuantizationTableId, mHorSampleFactor, mVerSampleFactor);
	}


	public int getComponentIndex()
	{
		return mComponentIndex;
	}


	public int getComponentId()
	{
		return mComponentId;
	}


	public void setComponentId(int aComponentId)
	{
		mComponentId = aComponentId;
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


//	public Type getComponentType()
//	{
//		return Type.fromComponentId(mComponentId);
//	}


	@Override
	public String toString()
	{
		return "ComponentInfo{" + "mComponentIndex=" + mComponentIndex + ", mComponentId=" + mComponentId + ", mQuantizationTableId=" + mQuantizationTableId + ", mHorSampleFactor=" + mHorSampleFactor + ", mVerSampleFactor=" + mVerSampleFactor + ", mTableDC=" + mTableDC + ", mTableAC=" + mTableAC + ", mComponentBlockOffset=" + mComponentBlockOffset + '}';
	}
}