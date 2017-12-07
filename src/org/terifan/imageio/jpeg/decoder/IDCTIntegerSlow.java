package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.DQTMarkerSegment;


public class IDCTIntegerSlow implements IDCT
{
	private final static int MAXJSAMPLE = 255;
	private final static int RANGE_CENTER = MAXJSAMPLE * 2 + 2;
	private final static int CONST_BITS = 13;
	private final static int PASS1_BITS = 2;
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


	@Override
	public void transform(int[] aCoefficients, DQTMarkerSegment aQuantizationTable)
	{
		double[] quantval = aQuantizationTable.getFloatDivisors();

		for (int i = 0; i < 64; i++)
		{
			aCoefficients[i] *= quantval[i];
		}

		transform(aCoefficients);
	}


	private void transform(int[] aCoefficients)
	{
		int[] workspace = new int[64];

		for (int ctr = 0; ctr < 8; ctr++)
		{
			if (aCoefficients[1 * 8 + ctr] == 0 && aCoefficients[2 * 8 + ctr] == 0 && aCoefficients[3 * 8 + ctr] == 0 && aCoefficients[4 * 8 + ctr] == 0 && aCoefficients[5 * 8 + ctr] == 0 && aCoefficients[6 * 8 + ctr] == 0 && aCoefficients[7 * 8 + ctr] == 0)
			{
				int dcval = aCoefficients[ctr] << PASS1_BITS;

				workspace[0 * 8 + ctr] = dcval;
				workspace[1 * 8 + ctr] = dcval;
				workspace[2 * 8 + ctr] = dcval;
				workspace[3 * 8 + ctr] = dcval;
				workspace[4 * 8 + ctr] = dcval;
				workspace[5 * 8 + ctr] = dcval;
				workspace[6 * 8 + ctr] = dcval;
				workspace[7 * 8 + ctr] = dcval;
				continue;
			}

			int z2 = aCoefficients[0 * 8 + ctr];
			int z3 = aCoefficients[4 * 8 + ctr];
			z2 <<= CONST_BITS;
			z3 <<= CONST_BITS;
			z2 += 1 << (CONST_BITS - PASS1_BITS - 1);

			int tmp0 = z2 + z3;
			int tmp1 = z2 - z3;

			z2 = aCoefficients[2 * 8 + ctr];
			z3 = aCoefficients[6 * 8 + ctr];

			int z1 = MULTIPLY(z2 + z3, FIX_0_541196100);
			int tmp2 = z1 + MULTIPLY(z2, FIX_0_765366865);
			int tmp3 = z1 - MULTIPLY(z3, FIX_1_847759065);

			int tmp10 = tmp0 + tmp2;
			int tmp13 = tmp0 - tmp2;
			int tmp11 = tmp1 + tmp3;
			int tmp12 = tmp1 - tmp3;

			tmp0 = aCoefficients[7 * 8 + ctr];
			tmp1 = aCoefficients[5 * 8 + ctr];
			tmp2 = aCoefficients[3 * 8 + ctr];
			tmp3 = aCoefficients[1 * 8 + ctr];

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

			workspace[0 * 8 + ctr] = RIGHT_SHIFT(tmp10 + tmp3, CONST_BITS - PASS1_BITS);
			workspace[7 * 8 + ctr] = RIGHT_SHIFT(tmp10 - tmp3, CONST_BITS - PASS1_BITS);
			workspace[1 * 8 + ctr] = RIGHT_SHIFT(tmp11 + tmp2, CONST_BITS - PASS1_BITS);
			workspace[6 * 8 + ctr] = RIGHT_SHIFT(tmp11 - tmp2, CONST_BITS - PASS1_BITS);
			workspace[2 * 8 + ctr] = RIGHT_SHIFT(tmp12 + tmp1, CONST_BITS - PASS1_BITS);
			workspace[5 * 8 + ctr] = RIGHT_SHIFT(tmp12 - tmp1, CONST_BITS - PASS1_BITS);
			workspace[3 * 8 + ctr] = RIGHT_SHIFT(tmp13 + tmp0, CONST_BITS - PASS1_BITS);
			workspace[4 * 8 + ctr] = RIGHT_SHIFT(tmp13 - tmp0, CONST_BITS - PASS1_BITS);
		}

		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			int z2 = workspace[0 + ctr] + (128 << (PASS1_BITS + 3)) + (1 << (PASS1_BITS + 2));

			if (workspace[1 + ctr] == 0 && workspace[2 + ctr] == 0 && workspace[3 + ctr] == 0 && workspace[4 + ctr] == 0 && workspace[5 + ctr] == 0 && workspace[6 + ctr] == 0 && workspace[7 + ctr] == 0)
			{
				int dcval = clamp(z2, PASS1_BITS + 3);

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

			int z3 = workspace[4 + ctr];
			
			int tmp0 = (z2 + z3) << CONST_BITS;
			int tmp1 = (z2 - z3) << CONST_BITS;

			z2 = workspace[2 + ctr];
			z3 = workspace[6 + ctr];

			int z1 = MULTIPLY(z2 + z3, FIX_0_541196100);
			int tmp2 = z1 + MULTIPLY(z2, FIX_0_765366865);
			int tmp3 = z1 - MULTIPLY(z3, FIX_1_847759065);

			int tmp10 = tmp0 + tmp2;
			int tmp13 = tmp0 - tmp2;
			int tmp11 = tmp1 + tmp3;
			int tmp12 = tmp1 - tmp3;

			tmp0 = workspace[7 + ctr];
			tmp1 = workspace[5 + ctr];
			tmp2 = workspace[3 + ctr];
			tmp3 = workspace[1 + ctr];

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

			aCoefficients[0 + ctr] = clamp(tmp10 + tmp3, CONST_BITS + PASS1_BITS + 3);
			aCoefficients[7 + ctr] = clamp(tmp10 - tmp3, CONST_BITS + PASS1_BITS + 3);
			aCoefficients[1 + ctr] = clamp(tmp11 + tmp2, CONST_BITS + PASS1_BITS + 3);
			aCoefficients[6 + ctr] = clamp(tmp11 - tmp2, CONST_BITS + PASS1_BITS + 3);
			aCoefficients[2 + ctr] = clamp(tmp12 + tmp1, CONST_BITS + PASS1_BITS + 3);
			aCoefficients[5 + ctr] = clamp(tmp12 - tmp1, CONST_BITS + PASS1_BITS + 3);
			aCoefficients[3 + ctr] = clamp(tmp13 + tmp0, CONST_BITS + PASS1_BITS + 3);
			aCoefficients[4 + ctr] = clamp(tmp13 - tmp0, CONST_BITS + PASS1_BITS + 3);
		}
	}


	private static int clamp(int x, int q)
	{
		return x >> q;
	}


	private static int MULTIPLY(int v, int q)
	{
		return v * q;
	}


	private static int RIGHT_SHIFT(int v, int q)
	{
		return v >> q;
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}
}
