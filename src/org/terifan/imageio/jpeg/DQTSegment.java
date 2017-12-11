package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;


public class DQTSegment
{
	public final static int PRECISION_8_BITS = 1;
	public final static int PRECISION_16_BITS = 2;

	private double[] mTableDbl = new double[64];
	private int mPrecision;
	private int mIdentity;


	public DQTSegment(int aIdentity, int... aQuantizationTable)
	{
		mIdentity = aIdentity;
		mPrecision = PRECISION_8_BITS;

		for (int i = 0; i < 64; i++)
		{
			mTableDbl[i] = aQuantizationTable[i];
		}
	}


	public DQTSegment(BitInputStream aInputStream) throws IOException
	{
		int temp = aInputStream.readInt8();
		mIdentity = temp & 0x07;
		mPrecision = (temp >> 3) == 0 ? PRECISION_8_BITS : PRECISION_16_BITS;

		for (int i = 0; i < 64; i++)
		{
			if (mPrecision == PRECISION_8_BITS)
			{
				mTableDbl[NATURAL_ORDER[i]] = aInputStream.readInt8();
			}
			else
			{
				mTableDbl[NATURAL_ORDER[i]] = aInputStream.readInt16() / 256.0;
			}
		}

		if (VERBOSE)
		{
			System.out.println("DQTMarkerSegment[identity=" + mIdentity + ", precision=" + (mPrecision == PRECISION_8_BITS ? 8 : 16) + "]");

			for (int row = 0, i = 0; row < 8; row++)
			{
				System.out.print(" ");
				for (int col = 0; col < 8; col++, i++)
				{
					System.out.printf("%7.3f ", mTableDbl[i]);
				}
				System.out.println();
			}
		}
	}


	public int getIdentity()
	{
		return mIdentity;
	}


	public int getPrecision()
	{
		return mPrecision;
	}


	public double[] getFloatDivisors()
	{
		return mTableDbl;
	}
}
