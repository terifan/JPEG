package org.terifan.imageio.jpeg.encoder;

import org.terifan.imageio.jpeg.DQTSegment.QuantizationTable;


/*
 * jfdctfst.c
 *
 * Copyright (C) 1994-1996, Thomas G. Lane.
 * Modified 2003-2009 by Guido Vollbeding.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains a fast, not so accurate integer implementation of the
 * forward DCT (Discrete Cosine Transform).
 *
 * A 2-D DCT can be done by 1-D DCT on each row followed by 1-D DCT
 * on each column.  Direct algorithms are also available, but they are
 * much more complex and seem not to be any faster when reduced to code.
 *
 * This implementation is based on Arai, Agui, and Nakajima's algorithm for
 * scaled DCT.  Their original paper (Trans. IEICE E-71(11):1095) is in
 * Japanese, but the algorithm is described in the Pennebaker & Mitchell
 * JPEG textbook (see REFERENCES section in file README).  The following code
 * is based directly on figure 4-8 in P&M.
 * While an 8-point DCT cannot be done in less than 11 multiplies, it is
 * possible to arrange the computation so that many of the multiplies are
 * simple scalings of the final outputs.  These multiplies can then be
 * folded into the multiplications or divisions by the JPEG quantization
 * table entries.  The AA&N method leaves only 5 multiplies and 29 adds
 * to be done in the DCT itself.
 * The primary disadvantage of this method is that with fixed-point math,
 * accuracy is lost due to imprecise representation of the scaled
 * quantization values.  The smaller the quantization table entry, the less
 * precise the scaled value, so this implementation does worse with high-
 * quality-setting files than with low-quality ones.
 */
public class FDCTIntegerFast implements FDCT
{
	private final static int CENTERJSAMPLE = 128;
	private final static int CONST_BITS = 8;
	private final static int FIX_0_382683433 = 98;
	private final static int FIX_0_541196100 = 139;
	private final static int FIX_0_707106781 = 181;
	private final static int FIX_1_306562965 = 334;

	private final static int[] AANSCALES = {
	  16384, 22725, 21407, 19266, 16384, 12873,  8867,  4520,
	  22725, 31521, 29692, 26722, 22725, 17855, 12299,  6270,
	  21407, 29692, 27969, 25172, 21407, 16819, 11585,  5906,
	  19266, 26722, 25172, 22654, 19266, 15137, 10426,  5315,
	  16384, 22725, 21407, 19266, 16384, 12873,  8867,  4520,
	  12873, 17855, 16819, 15137, 12873, 10114,  6967,  3552,
	   8867, 12299, 11585, 10426,  8867,  6967,  4799,  2446,
	   4520,  6270,  5906,  5315,  4520,  3552,  2446,  1247
	};


	@Override
	public void transform(int[] aCoefficients, QuantizationTable aQuantizationTable)
	{
		transform(aCoefficients);

		int[] quantval = aQuantizationTable.getDivisors();

		for (int i = 0; i < 64; i++)
		{
			aCoefficients[i] = (int)(aCoefficients[i] * 256L * 256 * 8 / quantval[i] / AANSCALES[i]);
		}
	}


	private void transform(int[] aCoefficients)
	{
		int[] workspace = new int[64];

		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			int tmp0 = aCoefficients[ctr + 0] + aCoefficients[ctr + 7];
			int tmp7 = aCoefficients[ctr + 0] - aCoefficients[ctr + 7];
			int tmp1 = aCoefficients[ctr + 1] + aCoefficients[ctr + 6];
			int tmp6 = aCoefficients[ctr + 1] - aCoefficients[ctr + 6];
			int tmp2 = aCoefficients[ctr + 2] + aCoefficients[ctr + 5];
			int tmp5 = aCoefficients[ctr + 2] - aCoefficients[ctr + 5];
			int tmp3 = aCoefficients[ctr + 3] + aCoefficients[ctr + 4];
			int tmp4 = aCoefficients[ctr + 3] - aCoefficients[ctr + 4];

			int tmp10 = tmp0 + tmp3;
			int tmp13 = tmp0 - tmp3;
			int tmp11 = tmp1 + tmp2;
			int tmp12 = tmp1 - tmp2;

			workspace[ctr + 0] = tmp10 + tmp11 - 8 * CENTERJSAMPLE;
			workspace[ctr + 4] = tmp10 - tmp11;

			int z1 = MULTIPLY(tmp12 + tmp13, FIX_0_707106781);
			workspace[ctr + 2] = tmp13 + z1;
			workspace[ctr + 6] = tmp13 - z1;

			tmp10 = tmp4 + tmp5;
			tmp11 = tmp5 + tmp6;
			tmp12 = tmp6 + tmp7;

			int z5 = MULTIPLY(tmp10 - tmp12, FIX_0_382683433);
			int z2 = MULTIPLY(tmp10, FIX_0_541196100) + z5;
			int z4 = MULTIPLY(tmp12, FIX_1_306562965) + z5;
			int z3 = MULTIPLY(tmp11, FIX_0_707106781);

			int z11 = tmp7 + z3;
			int z13 = tmp7 - z3;

			workspace[ctr + 5] = z13 + z2;
			workspace[ctr + 3] = z13 - z2;
			workspace[ctr + 1] = z11 + z4;
			workspace[ctr + 7] = z11 - z4;
		}

		for (int ctr = 0; ctr < 8; ctr++)
		{
			int tmp0 = workspace[8 * 0 + ctr] + workspace[8 * 7 + ctr];
			int tmp7 = workspace[8 * 0 + ctr] - workspace[8 * 7 + ctr];
			int tmp1 = workspace[8 * 1 + ctr] + workspace[8 * 6 + ctr];
			int tmp6 = workspace[8 * 1 + ctr] - workspace[8 * 6 + ctr];
			int tmp2 = workspace[8 * 2 + ctr] + workspace[8 * 5 + ctr];
			int tmp5 = workspace[8 * 2 + ctr] - workspace[8 * 5 + ctr];
			int tmp3 = workspace[8 * 3 + ctr] + workspace[8 * 4 + ctr];
			int tmp4 = workspace[8 * 3 + ctr] - workspace[8 * 4 + ctr];

			int tmp10 = tmp0 + tmp3;
			int tmp13 = tmp0 - tmp3;
			int tmp11 = tmp1 + tmp2;
			int tmp12 = tmp1 - tmp2;

			aCoefficients[ctr + 8 * 0] = (tmp10 + tmp11);
			aCoefficients[ctr + 8 * 4] = (tmp10 - tmp11);

			int z1 = MULTIPLY(tmp12 + tmp13, FIX_0_707106781);
			aCoefficients[ctr + 8 * 2] = (tmp13 + z1);
			aCoefficients[ctr + 8 * 6] = (tmp13 - z1);

			tmp10 = tmp4 + tmp5;
			tmp11 = tmp5 + tmp6;
			tmp12 = tmp6 + tmp7;

			int z5 = MULTIPLY(tmp10 - tmp12, FIX_0_382683433);
			int z2 = MULTIPLY(tmp10, FIX_0_541196100) + z5;
			int z4 = MULTIPLY(tmp12, FIX_1_306562965) + z5;
			int z3 = MULTIPLY(tmp11, FIX_0_707106781);

			int z11 = tmp7 + z3;
			int z13 = tmp7 - z3;

			aCoefficients[ctr + 8 * 5] = (z13 + z2);
			aCoefficients[ctr + 8 * 3] = (z13 - z2);
			aCoefficients[ctr + 8 * 1] = (z11 + z4);
			aCoefficients[ctr + 8 * 7] = (z11 - z4);
		}
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}


	private static int MULTIPLY(int x, int n)
	{
		return DESCALE(x * n, CONST_BITS);
	}
}
