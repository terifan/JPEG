package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.DQTSegment.QuantizationTable;


/**
 * This file contains a fast, not so accurate integer implementation of the inverse DCT (Discrete Cosine Transform).
 *
 * A 2-D IDCT can be done by 1-D IDCT on each column followed by 1-D IDCT on each row (or vice versa, but it's more convenient to emit a row
 * at a time). Direct algorithms are also available, but they are much more complex and seem not to be any faster when reduced to code.
 *
 * This implementation is based on Arai, Agui, and Nakajima's algorithm for scaled DCT. Their original paper (Trans. IEICE E-71(11):1095) is
 * in Japanese, but the algorithm is described in the Pennebaker & Mitchell JPEG textbook. The following code is based directly on figure
 * 4-8 in P&M. While an 8-point DCT cannot be done in less than 11 multiplies, it is possible to arrange the computation so that many of the
 * multiplies are simple scalings of the final outputs. These multiplies can then be folded into the multiplications or divisions by the
 * JPEG quantization table entries. The AA&N method leaves only 5 multiplies and 29 adds to be done in the DCT itself. The primary
 * disadvantage of this method is that with fixed-point math, accuracy is lost due to imprecise representation of the scaled quantization
 * values. The smaller the quantization table entry, the less precise the scaled value, so this implementation does worse with high-
 * quality-setting files than with low-quality ones.
 */
public class IDCTIntegerFast implements IDCT
{
	private final static int CONST_BITS = 8;
	private final static int PASS1_BITS = 2;

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

	private final static int FIX_1_082392200 = 277; // FIX(1.082392200)
	private final static int FIX_1_414213562 = 362;	// FIX(1.414213562)
	private final static int FIX_1_847759065 = 473;	// FIX(1.847759065)
	private final static int FIX_2_613125930 = 669;	// FIX(2.613125930)


	@Override
	public void transform(int[] aCoefficients, QuantizationTable aQuantizationTable)
	{
		int[] quantval = aQuantizationTable.getDivisors();

		for (int i = 0; i < 64; i++)
		{
			aCoefficients[i] = aCoefficients[i] * quantval[i] / 256 * AANSCALES[i] / 512;
		}

		transform(aCoefficients);
	}


	private void transform(int[] aCoefficients)
	{
		int[] workspace = new int[64];

		// Pass 1: process columns from input, store into work array.
		for (int ctr = 0; ctr < 8; ctr++)
		{
			if (aCoefficients[8 + ctr] == 0 && aCoefficients[16 + ctr] == 0 && aCoefficients[24 + ctr] == 0 && aCoefficients[32 + ctr] == 0 && aCoefficients[40 + ctr] == 0 && aCoefficients[48 + ctr] == 0 && aCoefficients[56 + ctr] == 0)
			{
				// AC terms all zero
				int dcval = aCoefficients[ctr];

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

			int tmp0 = aCoefficients[0 * 8 + ctr];
			int tmp1 = aCoefficients[2 * 8 + ctr];
			int tmp2 = aCoefficients[4 * 8 + ctr];
			int tmp3 = aCoefficients[6 * 8 + ctr];

			int tmp10 = tmp0 + tmp2;
			int tmp11 = tmp0 - tmp2;

			int tmp13 = tmp1 + tmp3;
			int tmp12 = MULTIPLY(tmp1 - tmp3, FIX_1_414213562) - tmp13;

			tmp0 = tmp10 + tmp13;
			tmp3 = tmp10 - tmp13;
			tmp1 = tmp11 + tmp12;
			tmp2 = tmp11 - tmp12;

			int tmp4 = aCoefficients[1 * 8 + ctr];
			int tmp5 = aCoefficients[3 * 8 + ctr];
			int tmp6 = aCoefficients[5 * 8 + ctr];
			int tmp7 = aCoefficients[7 * 8 + ctr];

			int z13 = tmp6 + tmp5;
			int z10 = tmp6 - tmp5;
			int z11 = tmp4 + tmp7;
			int z12 = tmp4 - tmp7;

			tmp7 = z11 + z13;
			tmp11 = MULTIPLY(z11 - z13, FIX_1_414213562);

			int z5 = MULTIPLY(z10 + z12, FIX_1_847759065);
			tmp10 = z5 - MULTIPLY(z12, FIX_1_082392200);
			tmp12 = z5 - MULTIPLY(z10, FIX_2_613125930);

			tmp6 = tmp12 - tmp7;
			tmp5 = tmp11 - tmp6;
			tmp4 = tmp10 - tmp5;

			workspace[0 * 8 + ctr] = tmp0 + tmp7;
			workspace[1 * 8 + ctr] = tmp1 + tmp6;
			workspace[2 * 8 + ctr] = tmp2 + tmp5;
			workspace[3 * 8 + ctr] = tmp3 + tmp4;
			workspace[4 * 8 + ctr] = tmp3 - tmp4;
			workspace[5 * 8 + ctr] = tmp2 - tmp5;
			workspace[6 * 8 + ctr] = tmp1 - tmp6;
			workspace[7 * 8 + ctr] = tmp0 - tmp7;
		}

		// Pass 2: process rows from work array, store into output array.
		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			int z5 = workspace[ctr] + ((128 << (PASS1_BITS + 3)) + (1 << (PASS1_BITS + 2)));

			if (workspace[ctr + 1] == 0 && workspace[ctr + 2] == 0 && workspace[ctr + 3] == 0 && workspace[ctr + 4] == 0 && workspace[ctr + 5] == 0 && workspace[ctr + 6] == 0 && workspace[ctr + 7] == 0)
			{
				// AC terms all zero
				int dcval = CLAMP(workspace[ctr], PASS1_BITS + 3);

				aCoefficients[ctr + 0] = dcval;
				aCoefficients[ctr + 1] = dcval;
				aCoefficients[ctr + 2] = dcval;
				aCoefficients[ctr + 3] = dcval;
				aCoefficients[ctr + 4] = dcval;
				aCoefficients[ctr + 5] = dcval;
				aCoefficients[ctr + 6] = dcval;
				aCoefficients[ctr + 7] = dcval;

				continue;
			}

			int tmp10 = z5 + workspace[ctr + 4];
			int tmp11 = z5 - workspace[ctr + 4];

			int tmp13 = workspace[ctr + 2] + workspace[ctr + 6];
			int tmp12 = MULTIPLY(workspace[ctr + 2] - workspace[ctr + 6], FIX_1_414213562) - tmp13;

			int tmp0 = tmp10 + tmp13;
			int tmp3 = tmp10 - tmp13;
			int tmp1 = tmp11 + tmp12;
			int tmp2 = tmp11 - tmp12;

			int z13 = workspace[ctr + 5] + workspace[ctr + 3];
			int z10 = workspace[ctr + 5] - workspace[ctr + 3];
			int z11 = workspace[ctr + 1] + workspace[ctr + 7];
			int z12 = workspace[ctr + 1] - workspace[ctr + 7];

			int tmp7 = z11 + z13;
			tmp11 = MULTIPLY(z11 - z13, FIX_1_414213562);

			z5 = MULTIPLY((z10 + z12), FIX_1_847759065);
			tmp10 = z5 - MULTIPLY(z12, FIX_1_082392200);
			tmp12 = z5 - MULTIPLY(z10, FIX_2_613125930);

			int tmp6 = tmp12 - tmp7;
			int tmp5 = tmp11 - tmp6;
			int tmp4 = tmp10 - tmp5;

			// Final output stage: scale down by a factor of 8
			aCoefficients[ctr + 0] = CLAMP(tmp0 + tmp7, PASS1_BITS + 3);
			aCoefficients[ctr + 1] = CLAMP(tmp1 + tmp6, PASS1_BITS + 3);
			aCoefficients[ctr + 2] = CLAMP(tmp2 + tmp5, PASS1_BITS + 3);
			aCoefficients[ctr + 3] = CLAMP(tmp3 + tmp4, PASS1_BITS + 3);
			aCoefficients[ctr + 4] = CLAMP(tmp3 - tmp4, PASS1_BITS + 3);
			aCoefficients[ctr + 5] = CLAMP(tmp2 - tmp5, PASS1_BITS + 3);
			aCoefficients[ctr + 6] = CLAMP(tmp1 - tmp6, PASS1_BITS + 3);
			aCoefficients[ctr + 7] = CLAMP(tmp0 - tmp7, PASS1_BITS + 3);
		}
	}


	private static int CLAMP(int x, int n)
	{
//		x = 128 + DESCALE(x, n + 3);
//		return x < 0 ? 0 : x > 255 ? 255 : x;

		return x >> (n + 3);
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}


	private static int MULTIPLY(int x, int n)
	{
		return (x * n) >> CONST_BITS;
	}
}
