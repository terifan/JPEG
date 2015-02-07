package org.terifan.multimedia.jpeg;

/**
 * This file contains a fast, not so accurate integer implementation of the
 * inverse DCT (Discrete Cosine Transform).
 *
 * A 2-D IDCT can be done by 1-D IDCT on each column followed by 1-D IDCT
 * on each row (or vice versa, but it's more convenient to emit a row at
 * a time).  Direct algorithms are also available, but they are much more
 * complex and seem not to be any faster when reduced to code.
 *
 * This implementation is based on Arai, Agui, and Nakajima's algorithm for
 * scaled DCT.  Their original paper (Trans. IEICE E-71(11):1095) is in
 * Japanese, but the algorithm is described in the Pennebaker & Mitchell
 * JPEG textbook. The following code is based directly on figure 4-8 in P&M.
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
class IDCTInteger
{
	/**
	 * Perform dequantization and inverse DCT on one block of coefficients.
	 */
	public static void transform(int[] aWorkspace, int[] aCoefficients, int[] aQuantizationTable)
	{
		// Pass 1: process columns from input, store into work array. 
		for (int ctr = 0; ctr < 8; ctr++)
		{
			if (aCoefficients[8 + ctr] == 0 && aCoefficients[16 + ctr] == 0 && aCoefficients[24 + ctr] == 0 && aCoefficients[32 + ctr] == 0
					&& aCoefficients[40 + ctr] == 0 && aCoefficients[48 + ctr] == 0 && aCoefficients[56 + ctr] == 0)
			{
				// AC terms all zero
				int dcval = aCoefficients[ctr] * aQuantizationTable[ctr];

				aWorkspace[     ctr] = dcval;
				aWorkspace[ 8 + ctr] = dcval;
				aWorkspace[16 + ctr] = dcval;
				aWorkspace[24 + ctr] = dcval;
				aWorkspace[32 + ctr] = dcval;
				aWorkspace[40 + ctr] = dcval;
				aWorkspace[48 + ctr] = dcval;
				aWorkspace[56 + ctr] = dcval;

				continue;
			}

			int tmp0 = aCoefficients[     ctr] * aQuantizationTable[     ctr];
			int tmp1 = aCoefficients[16 + ctr] * aQuantizationTable[16 + ctr];
			int tmp2 = aCoefficients[32 + ctr] * aQuantizationTable[32 + ctr];
			int tmp3 = aCoefficients[48 + ctr] * aQuantizationTable[48 + ctr];

			int tmp10 = tmp0 + tmp2;
			int tmp11 = tmp0 - tmp2;

			int tmp13 = tmp1 + tmp3;
			int tmp12 = (((tmp1 - tmp3) * 362) >> 8) - tmp13;

			tmp0 = tmp10 + tmp13;
			tmp3 = tmp10 - tmp13;
			tmp1 = tmp11 + tmp12;
			tmp2 = tmp11 - tmp12;

			int tmp4 = aCoefficients[ 8 + ctr] * aQuantizationTable[ 8 + ctr];
			int tmp5 = aCoefficients[24 + ctr] * aQuantizationTable[24 + ctr];
			int tmp6 = aCoefficients[40 + ctr] * aQuantizationTable[40 + ctr];
			int tmp7 = aCoefficients[56 + ctr] * aQuantizationTable[56 + ctr];

			int z13 = tmp6 + tmp5;
			int z10 = tmp6 - tmp5;
			int z11 = tmp4 + tmp7;
			int z12 = tmp4 - tmp7;

			tmp7 = z11 + z13;
			tmp11 = ((z11 - z13) * 362) >> 8;

			int z5 = ((z10 + z12) * 473) >> 8;
			tmp10 = ((z12 * 277) >> 8) - z5;
			tmp12 = ((z10 * -669) >> 8) + z5;

			tmp6 = tmp12 - tmp7;
			tmp5 = tmp11 - tmp6;
			tmp4 = tmp10 + tmp5;

			aWorkspace[     ctr] = tmp0 + tmp7;
			aWorkspace[ 8 + ctr] = tmp1 + tmp6;
			aWorkspace[16 + ctr] = tmp2 + tmp5;
			aWorkspace[24 + ctr] = tmp3 - tmp4;
			aWorkspace[32 + ctr] = tmp3 + tmp4;
			aWorkspace[40 + ctr] = tmp2 - tmp5;
			aWorkspace[48 + ctr] = tmp1 - tmp6;
			aWorkspace[56 + ctr] = tmp0 - tmp7;
		}

		// Pass 2: process rows from work array, store into output array. 
		for (int ctr = 0; ctr < 8; ctr++)
		{
			int offset = ctr * 8;

			if (aWorkspace[offset + 1] == 0 && aWorkspace[offset + 2] == 0 && aWorkspace[offset + 3] == 0 && aWorkspace[offset + 4] == 0
			 && aWorkspace[offset + 5] == 0 && aWorkspace[offset + 6] == 0 && aWorkspace[offset + 7] == 0)
			 {
				// AC terms all zero
				int dcval = (aWorkspace[offset] >> 11) + 128;

				aCoefficients[offset + 0] = dcval;
				aCoefficients[offset + 1] = dcval;
				aCoefficients[offset + 2] = dcval;
				aCoefficients[offset + 3] = dcval;
				aCoefficients[offset + 4] = dcval;
				aCoefficients[offset + 5] = dcval;
				aCoefficients[offset + 6] = dcval;
				aCoefficients[offset + 7] = dcval;

				continue;
			 }

			int tmp10 = aWorkspace[offset] + aWorkspace[offset + 4];
			int tmp11 = aWorkspace[offset] - aWorkspace[offset + 4];

			int tmp13 = aWorkspace[offset + 2] + aWorkspace[offset + 6];
			int tmp12 = (((aWorkspace[offset + 2] - aWorkspace[offset + 6]) * 362) >> 8) - tmp13;

			int tmp0 = tmp10 + tmp13;
			int tmp3 = tmp10 - tmp13;
			int tmp1 = tmp11 + tmp12;
			int tmp2 = tmp11 - tmp12;

			int z13 = aWorkspace[offset + 5] + aWorkspace[offset + 3];
			int z10 = aWorkspace[offset + 5] - aWorkspace[offset + 3];
			int z11 = aWorkspace[offset + 1] + aWorkspace[offset + 7];
			int z12 = aWorkspace[offset + 1] - aWorkspace[offset + 7];

			int tmp7 = z11 + z13;
			tmp11 = ((z11 - z13) * 362) >> 8;

			int z5 = ((z10 + z12) * 473) >> 8;
			tmp10 = ((z12 * 277) >> 8) - z5;
			tmp12 = ((z10 * -669) >> 8) + z5;

			int tmp6 = tmp12 - tmp7;
			int tmp5 = tmp11 - tmp6;
			int tmp4 = tmp10 + tmp5;

			// Final output stage: scale down by a factor of 8
			aCoefficients[offset + 0] = ((tmp0 + tmp7) >> 11) + 128;
			aCoefficients[offset + 1] = ((tmp1 + tmp6) >> 11) + 128;
			aCoefficients[offset + 2] = ((tmp2 + tmp5) >> 11) + 128;
			aCoefficients[offset + 3] = ((tmp3 - tmp4) >> 11) + 128;
			aCoefficients[offset + 4] = ((tmp3 + tmp4) >> 11) + 128;
			aCoefficients[offset + 5] = ((tmp2 - tmp5) >> 11) + 128;
			aCoefficients[offset + 6] = ((tmp1 - tmp6) >> 11) + 128;
			aCoefficients[offset + 7] = ((tmp0 - tmp7) >> 11) + 128;
		}
	}
}