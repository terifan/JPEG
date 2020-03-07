package org.terifan.imageio.jpeg;

import java.io.PrintStream;



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


	public void print(Log aLog)
	{
		for (int row = 0, i = 0; row < 8; row++)
		{
			aLog.print("  ");
			for (int col = 0; col < 8; col++, i++)
			{
				aLog.printf("%7.3f ", mTable[i]);
			}
			aLog.println();
		}
	}
}
