package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.DQTMarkerSegment;


public class IDCTInteger implements IDCT
{
	private final static int CONST_BITS = 13;
	private final static int PASS1_BITS = 1;

	private final static int FIX_0_298631336 = 2446;
	private final static int FIX_0_390180644 = 3196;
	private final static int FIX_0_541196100 = 4433;
	private final static int FIX_0_765366865 = 6270;
	private final static int FIX_0_899976223 = 7373;
	private final static int FIX_1_175875602 = 9633;
	private final static int FIX_1_501321110 = 12299;
	private final static int FIX_1_847759065 = 15137;
	private final static int FIX_1_961570560 = 16069;
	private final static int FIX_2_053119869 = 16819;
	private final static int FIX_2_562915447 = 20995;
	private final static int FIX_3_072711026 = 25172;


	/**
	 * Perform dequantization and inverse DCT on one block of coefficients.
	 */
	@Override
	public void transform(int[] aCoefficients, DQTMarkerSegment aQuantizationTable)
	{
		int[] wsptr = new int[64];
		int[] quantptr = aQuantizationTable.getTableInt();

		for (int ctr = 0; ctr < 8; ctr++)
		{
			if (aCoefficients[8 + ctr] == 0 && aCoefficients[16 + ctr] == 0 && aCoefficients[24 + ctr] == 0 && aCoefficients[32 + ctr] == 0 && aCoefficients[40 + ctr] == 0 && aCoefficients[48 + ctr] == 0 && aCoefficients[56 + ctr] == 0)
			{
				int dcval = (aCoefficients[ctr] * quantptr[ctr]) << PASS1_BITS;

				wsptr[ctr] = dcval;
				wsptr[8 + ctr] = dcval;
				wsptr[16 + ctr] = dcval;
				wsptr[24 + ctr] = dcval;
				wsptr[32 + ctr] = dcval;
				wsptr[40 + ctr] = dcval;
				wsptr[48 + ctr] = dcval;
				wsptr[56 + ctr] = dcval;
				continue;
			}

			int z2 = DEQUANTIZE(aCoefficients[16 + ctr], quantptr[16 + ctr]);
			int z3 = DEQUANTIZE(aCoefficients[48 + ctr], quantptr[48 + ctr]);

			int z1 = MULTIPLY(z2 + z3, FIX_0_541196100);
			int tmp2 = z1 + MULTIPLY(z2, FIX_0_765366865);
			int tmp3 = z1 - MULTIPLY(z3, FIX_1_847759065);

			z2 = DEQUANTIZE(aCoefficients[ctr], quantptr[ctr]);
			z3 = DEQUANTIZE(aCoefficients[32 + ctr], quantptr[32 + ctr]);
			z2 <<= CONST_BITS;
			z3 <<= CONST_BITS;
			z2 += 1 << (CONST_BITS - PASS1_BITS - 1);

			int tmp0 = z2 + z3;
			int tmp1 = z2 - z3;

			int tmp10 = tmp0 + tmp2;
			int tmp13 = tmp0 - tmp2;
			int tmp11 = tmp1 + tmp3;
			int tmp12 = tmp1 - tmp3;

			tmp0 = DEQUANTIZE(aCoefficients[56 + ctr], quantptr[56 + ctr]);
			tmp1 = DEQUANTIZE(aCoefficients[40 + ctr], quantptr[40 + ctr]);
			tmp2 = DEQUANTIZE(aCoefficients[24 + ctr], quantptr[24 + ctr]);
			tmp3 = DEQUANTIZE(aCoefficients[8 + ctr], quantptr[8 + ctr]);

			z2 = tmp0 + tmp2;
			z3 = tmp1 + tmp3;

			z1 = MULTIPLY(z2 + z3, FIX_1_175875602);
			z2 = MULTIPLY(z2, -FIX_1_961570560);
			z3 = MULTIPLY(z3, -FIX_0_390180644);
			z2 += z1;
			z3 += z1;

			z1 = MULTIPLY(tmp0 + tmp3, -FIX_0_899976223);
			tmp0 = MULTIPLY(tmp0, FIX_0_298631336);
			tmp3 = MULTIPLY(tmp3, FIX_1_501321110);
			tmp0 += z1 + z2;
			tmp3 += z1 + z3;

			z1 = MULTIPLY(tmp1 + tmp2, -FIX_2_562915447);
			tmp1 = MULTIPLY(tmp1, FIX_2_053119869);
			tmp2 = MULTIPLY(tmp2, FIX_3_072711026);
			tmp1 += z1 + z3;
			tmp2 += z1 + z2;

			wsptr[ctr] = RIGHT_SHIFT(tmp10 + tmp3, CONST_BITS - PASS1_BITS);
			wsptr[56 + ctr] = RIGHT_SHIFT(tmp10 - tmp3, CONST_BITS - PASS1_BITS);
			wsptr[8 + ctr] = RIGHT_SHIFT(tmp11 + tmp2, CONST_BITS - PASS1_BITS);
			wsptr[48 + ctr] = RIGHT_SHIFT(tmp11 - tmp2, CONST_BITS - PASS1_BITS);
			wsptr[16 + ctr] = RIGHT_SHIFT(tmp12 + tmp1, CONST_BITS - PASS1_BITS);
			wsptr[40 + ctr] = RIGHT_SHIFT(tmp12 - tmp1, CONST_BITS - PASS1_BITS);
			wsptr[24 + ctr] = RIGHT_SHIFT(tmp13 + tmp0, CONST_BITS - PASS1_BITS);
			wsptr[32 + ctr] = RIGHT_SHIFT(tmp13 - tmp0, CONST_BITS - PASS1_BITS);
		}

		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			if (wsptr[1 + ctr] == 0 && wsptr[2 + ctr] == 0 && wsptr[3 + ctr] == 0 && wsptr[4 + ctr] == 0 && wsptr[5 + ctr] == 0 && wsptr[6 + ctr] == 0 && wsptr[7 + ctr] == 0)
			{
				int dcval = clamp(DESCALE(wsptr[ctr], PASS1_BITS + 3));

				aCoefficients[0 + ctr] = dcval;
				aCoefficients[1 + ctr] = dcval;
				aCoefficients[2 + ctr] = dcval;
				aCoefficients[3 + ctr] = dcval;
				aCoefficients[4 + ctr] = dcval;
				aCoefficients[5 + ctr] = dcval;
				aCoefficients[6 + ctr] = dcval;
				aCoefficients[7 + ctr] = dcval;
				continue;
			}

			int z2 = wsptr[2 + ctr];
			int z3 = wsptr[6 + ctr];

			int z1 = MULTIPLY(z2 + z3, FIX_0_541196100);
			int tmp2 = z1 + MULTIPLY(z2, FIX_0_765366865);
			int tmp3 = z1 - MULTIPLY(z3, FIX_1_847759065);

			z2 = wsptr[0 + ctr] + (1 << (PASS1_BITS + 2));
			z3 = wsptr[4 + ctr];

			int tmp0 = (z2 + z3) << CONST_BITS;
			int tmp1 = (z2 - z3) << CONST_BITS;

			int tmp10 = tmp0 + tmp2;
			int tmp13 = tmp0 - tmp2;
			int tmp11 = tmp1 + tmp3;
			int tmp12 = tmp1 - tmp3;

			tmp0 = wsptr[7 + ctr];
			tmp1 = wsptr[5 + ctr];
			tmp2 = wsptr[3 + ctr];
			tmp3 = wsptr[1 + ctr];

			z2 = tmp0 + tmp2;
			z3 = tmp1 + tmp3;

			z1 = MULTIPLY(z2 + z3, FIX_1_175875602);
			z2 = MULTIPLY(z2, -FIX_1_961570560);
			z3 = MULTIPLY(z3, -FIX_0_390180644);
			z2 += z1;
			z3 += z1;

			z1 = MULTIPLY(tmp0 + tmp3, -FIX_0_899976223);
			tmp0 = MULTIPLY(tmp0, FIX_0_298631336);
			tmp3 = MULTIPLY(tmp3, FIX_1_501321110);
			tmp0 += z1 + z2;
			tmp3 += z1 + z3;

			z1 = MULTIPLY(tmp1 + tmp2, -FIX_2_562915447);
			tmp1 = MULTIPLY(tmp1, FIX_2_053119869);
			tmp2 = MULTIPLY(tmp2, FIX_3_072711026);
			tmp1 += z1 + z3;
			tmp2 += z1 + z2;

			aCoefficients[0 + ctr] = clamp(RIGHT_SHIFT(tmp10 + tmp3, CONST_BITS + PASS1_BITS + 3));
			aCoefficients[7 + ctr] = clamp(RIGHT_SHIFT(tmp10 - tmp3, CONST_BITS + PASS1_BITS + 3));
			aCoefficients[1 + ctr] = clamp(RIGHT_SHIFT(tmp11 + tmp2, CONST_BITS + PASS1_BITS + 3));
			aCoefficients[6 + ctr] = clamp(RIGHT_SHIFT(tmp11 - tmp2, CONST_BITS + PASS1_BITS + 3));
			aCoefficients[2 + ctr] = clamp(RIGHT_SHIFT(tmp12 + tmp1, CONST_BITS + PASS1_BITS + 3));
			aCoefficients[5 + ctr] = clamp(RIGHT_SHIFT(tmp12 - tmp1, CONST_BITS + PASS1_BITS + 3));
			aCoefficients[3 + ctr] = clamp(RIGHT_SHIFT(tmp13 + tmp0, CONST_BITS + PASS1_BITS + 3));
			aCoefficients[4 + ctr] = clamp(RIGHT_SHIFT(tmp13 - tmp0, CONST_BITS + PASS1_BITS + 3));
		}
	}


	private static int clamp(int aValue)
	{
		aValue = 128 + (aValue >> 5);

		return aValue < 0 ? 0 : aValue > 255 ? 255 : aValue;
	}


	private final static int DEQUANTIZE(int v, int q)
	{
		return v * q;
	}


	private final static int MULTIPLY(int v, int q)
	{
		return v * q;
	}


	private final static int RIGHT_SHIFT(int v, int q)
	{
		return v >> q;
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}
}
