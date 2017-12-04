package org.terifan.multimedia.jpeg;

import java.io.IOException;
import static org.terifan.multimedia.jpeg.JPEGImageReader.ZIGZAG;


class DQTMarkerSegment
{
	public final static int PRECISION_8_BITS = 1;
	public final static int PRECISION_16_BITS = 2;

    private static final int[] k1 = {
        16,  11,  10,  16,  24,  40,  51,  61,
        12,  12,  14,  19,  26,  58,  60,  55,
        14,  13,  16,  24,  40,  57,  69,  56,
        14,  17,  22,  29,  51,  87,  80,  62,
        18,  22,  37,  56,  68,  109, 103, 77,
        24,  35,  55,  64,  81,  104, 113, 92,
        49,  64,  78,  87,  103, 121, 120, 101,
        72,  92,  95,  98,  112, 100, 103, 99,
    };

    private static final int[] k1div2 = {
        8,   6,   5,   8,   12,  20,  26,  31,
        6,   6,   7,   10,  13,  29,  30,  28,
        7,   7,   8,   12,  20,  29,  35,  28,
        7,   9,   11,  15,  26,  44,  40,  31,
        9,   11,  19,  28,  34,  55,  52,  39,
        12,  18,  28,  32,  41,  52,  57,  46,
        25,  32,  39,  44,  52,  61,  60,  51,
        36,  46,  48,  49,  56,  50,  52,  50,
    };

    private static final int[] k2 = {
        17,  18,  24,  47,  99,  99,  99,  99,
        18,  21,  26,  66,  99,  99,  99,  99,
        24,  26,  56,  99,  99,  99,  99,  99,
        47,  66,  99,  99,  99,  99,  99,  99,
        99,  99,  99,  99,  99,  99,  99,  99,
        99,  99,  99,  99,  99,  99,  99,  99,
        99,  99,  99,  99,  99,  99,  99,  99,
        99,  99,  99,  99,  99,  99,  99,  99,
    };

    private static final int[] k2div2 = {
        9,   9,   12,  24,  50,  50,  50,  50,
        9,   11,  13,  33,  50,  50,  50,  50,
        12,  13,  28,  50,  50,  50,  50,  50,
        24,  33,  50,  50,  50,  50,  50,  50,
        50,  50,  50,  50,  50,  50,  50,  50,
        50,  50,  50,  50,  50,  50,  50,  50,
        50,  50,  50,  50,  50,  50,  50,  50,
        50,  50,  50,  50,  50,  50,  50,  50,
    };

	private double[] mTableDbl = new double[64];
	private int[] mTableInt = new int[64];
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
				mTableDbl[ZIGZAG[i]] = aInputStream.readInt8();
			}
			else
			{
				mTableDbl[ZIGZAG[i]] = aInputStream.readInt16() / 256.0;
			}
		}

		double[] scaleFactors =
		{
			1.0, 1.387039845, 1.306562965, 1.175875602,
			1.0, 0.785694958, 0.541196100, 0.275899379
		};

		for (int row = 0, i = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++, i++)
			{
				mTableDbl[i] *= scaleFactors[row] * scaleFactors[col] / 8.0;
				mTableInt[i] = Math.min(Math.max((int)(256 * mTableDbl[i] + 0.5), 1), 255);
			}
		}

		if (JPEGImageReader.VERBOSE)
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


	public int[] getTableInt()
	{
		return mTableInt;
	}


	public double[] getTableDbl()
	{
		return mTableDbl;
	}
}
