package org.terifan.imageio.jpeg;


public class QuantizationTable
{
	public final static int PRECISION_8_BITS = 1;
	public final static int PRECISION_16_BITS = 2;

	/**
	 * 16-bit quantization values.
	 */
	private int[] mTable;
	private int mPrecision;
	private int mIdentity;


	/**
	 *
	 * @param aIdentity
	 * @param aPrecision
	 * @param aTable
	 *    8 or 16 bit values depending on the precision parameter, 8 bit values will be scaled to 16 bits.
	 */
	public QuantizationTable(int aIdentity, int aPrecision, int... aTable)
	{
		mIdentity = aIdentity;
		mPrecision = aPrecision;
		mTable = new int[64];

		boolean b = aPrecision == PRECISION_8_BITS;

		for (int i = 0; i < 64; i++)
		{
			mTable[i] = b ? aTable[i] << 8 : aTable[i];
		}
	}


	public int[] getDivisors()
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
				aLog.print("%7.3f ", mTable[i]);
			}
			aLog.println("");
		}
	}
}
