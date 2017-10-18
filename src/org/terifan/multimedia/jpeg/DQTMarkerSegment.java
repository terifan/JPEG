package org.terifan.multimedia.jpeg;

import java.io.IOException;
import static org.terifan.multimedia.jpeg.JPEGImageReader.ZIGZAG;


class DQTMarkerSegment
{
	public final static int PRECISION_8_BITS = 1;
	public final static int PRECISION_16_BITS = 2;

	private double[] mTableD = new double[64];
//	private int[] mTableI = new int[64];
	private int mPrecision;
	private int mIdentity;


	public DQTMarkerSegment(BitInputStream aInputStream) throws IOException
	{
		int temp = aInputStream.readInt8();
		mIdentity = temp & 0x07;
		mPrecision = (temp >> 3) == 0 ? PRECISION_8_BITS : PRECISION_16_BITS;

		for (int i = 0; i < 64; i++)
		{
			if (mPrecision == PRECISION_8_BITS)
			{
				mTableD[ZIGZAG[i]] = aInputStream.readInt8() / 255.0;
			}
			else
			{
				mTableD[ZIGZAG[i]] = aInputStream.readInt16() / 65535.0;
			}
		}

		double[] scaleFactors =
		{
			1.0, 1.387039845, 1.306562965, 1.175875602, 1.0, 0.785694958, 0.541196100, 0.275899379
		};

		for (int row = 0, i = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++, i++)
			{
				mTableD[i] *= scaleFactors[row] * scaleFactors[col] * 16 * 16;
//				mTableI[i] = (int)Math.round(255 * mTableD[i]);
			}
		}

		if (JPEGImageReader.VERBOSE)
		{
			System.out.println("DQTMarkerSegment[identity=" + mIdentity + ", precision=" + (mPrecision == PRECISION_8_BITS ? "8bit" : "16bit") + "]");
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


//	public int[] getTable()
//	{
//		return mTableI;
//	}


	public double[] getTable()
	{
		return mTableD;
	}
}
