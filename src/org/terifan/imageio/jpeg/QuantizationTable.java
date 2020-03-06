package org.terifan.imageio.jpeg;


public class QuantizationTable 
{
	public final static int PRECISION_8_BITS = 1;
	public final static int PRECISION_16_BITS = 2;

	private double[] mTable;
	private int mPrecision;
	private int mIdentity;


	public QuantizationTable(int aIdentity, int aPrecision, int... aTable)
	{
		mIdentity = aIdentity;
		mPrecision = aPrecision;

		mTable = new double[64];

		for (int i = 0; i < 64; i++)
		{
			mTable[i] = aTable[i];
		}
	}


	public QuantizationTable(int aIdentity, int aPrecision, double... aTable)
	{
		mIdentity = aIdentity;
		mPrecision = aPrecision;
		mTable = aTable;
	}
	
	
	public double[] getDivisors()
	{
		return mTable;
	}


	public int getIdentity()
	{
		return mIdentity;
	}


	public int getPrecision()
	{
		return mPrecision;
	}
}
