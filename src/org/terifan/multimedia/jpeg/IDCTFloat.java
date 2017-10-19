package org.terifan.multimedia.jpeg;


/**
 * This file contains a floating-point implementation of the inverse DCT (Discrete Cosine Transform).
 *
 * A 2-D IDCT can be done by 1-D IDCT on each column followed by 1-D IDCT on each row (or vice versa, but it's more convenient to emit a row
 * at a time). Direct algorithms are also available, but they are much more complex and seem not to be any faster when reduced to code.
 *
 * This implementation is based on Arai, Agui, and Nakajima's algorithm for scaled DCT. Their original paper (Trans. IEICE E-71(11):1095) is
 * in Japanese, but the algorithm is described in the Pennebaker & Mitchell JPEG textbook (see REFERENCES section in file README). The
 * following code is based directly on figure 4-8 in P&M. While an 8-point DCT cannot be done in less than 11 multiplies, it is possible to
 * arrange the computation so that many of the multiplies are simple scalings of the final outputs. These multiplies can then be folded into
 * the multiplications or divisions by the JPEG quantization table entries. The AA&N method leaves only 5 multiplies and 29 adds to be done
 * in the DCT itself. The primary disadvantage of this method is that with a fixed-point implementation, accuracy is lost due to imprecise
 * representation of the scaled quantization values. However, that problem does not arise if we use floating point arithmetic.
 */
class IDCTFloat
{
	private final double[] mWorkspace = new double[64];


	/**
	 * Perform dequantization and inverse DCT on one block of coefficients.
	 */
	public void transform(int[] aCoefficients, double[] aQuantizationTable)
	{
		// Pass 1: process columns from input, store into work array.
		for (int ctr = 0; ctr < 8; ctr++)
		{
			if (aCoefficients[8 + ctr] == 0 && aCoefficients[16 + ctr] == 0 && aCoefficients[24 + ctr] == 0 && aCoefficients[32 + ctr] == 0 && aCoefficients[40 + ctr] == 0 && aCoefficients[48 + ctr] == 0 && aCoefficients[56 + ctr] == 0)
			{
				// AC terms all zero
				double dcval = aCoefficients[ctr] * aQuantizationTable[ctr];

				mWorkspace[ctr] = dcval;
				mWorkspace[8 + ctr] = dcval;
				mWorkspace[16 + ctr] = dcval;
				mWorkspace[24 + ctr] = dcval;
				mWorkspace[32 + ctr] = dcval;
				mWorkspace[40 + ctr] = dcval;
				mWorkspace[48 + ctr] = dcval;
				mWorkspace[56 + ctr] = dcval;

				continue;
			}

			double tmp0 = aCoefficients[0 + ctr] * aQuantizationTable[0 + ctr];
			double tmp1 = aCoefficients[16 + ctr] * aQuantizationTable[16 + ctr];
			double tmp2 = aCoefficients[32 + ctr] * aQuantizationTable[32 + ctr];
			double tmp3 = aCoefficients[48 + ctr] * aQuantizationTable[48 + ctr];

			double tmp10 = tmp0 + tmp2;
			double tmp11 = tmp0 - tmp2;

			double tmp13 = tmp1 + tmp3;
			double tmp12 = (tmp1 - tmp3) * 1.414213562 - tmp13;

			tmp0 = tmp10 + tmp13;
			tmp3 = tmp10 - tmp13;
			tmp1 = tmp11 + tmp12;
			tmp2 = tmp11 - tmp12;

			double tmp4 = aCoefficients[8 + ctr] * aQuantizationTable[8 + ctr];
			double tmp5 = aCoefficients[24 + ctr] * aQuantizationTable[24 + ctr];
			double tmp6 = aCoefficients[40 + ctr] * aQuantizationTable[40 + ctr];
			double tmp7 = aCoefficients[56 + ctr] * aQuantizationTable[56 + ctr];

			double z13 = tmp6 + tmp5;
			double z10 = tmp6 - tmp5;
			double z11 = tmp4 + tmp7;
			double z12 = tmp4 - tmp7;

			tmp7 = z11 + z13;
			tmp11 = (z11 - z13) * 1.414213562;

			double z5 = (z10 + z12) * 1.847759065;
			tmp10 = 1.0823922 * z12 - z5;
			tmp12 = -2.61312593 * z10 + z5;

			tmp6 = tmp12 - tmp7;
			tmp5 = tmp11 - tmp6;
			tmp4 = tmp10 + tmp5;

			mWorkspace[ctr] = tmp0 + tmp7;
			mWorkspace[8 + ctr] = tmp1 + tmp6;
			mWorkspace[16 + ctr] = tmp2 + tmp5;
			mWorkspace[24 + ctr] = tmp3 - tmp4;
			mWorkspace[32 + ctr] = tmp3 + tmp4;
			mWorkspace[40 + ctr] = tmp2 - tmp5;
			mWorkspace[48 + ctr] = tmp1 - tmp6;
			mWorkspace[56 + ctr] = tmp0 - tmp7;
		}

		// Pass 2: process rows from work array, store into output array.
		for (int ctr = 0, offset = 0; ctr < 8; ctr++, offset += 8)
		{
			double tmp10 = mWorkspace[offset] + mWorkspace[offset + 4];
			double tmp11 = mWorkspace[offset] - mWorkspace[offset + 4];

			double tmp13 = mWorkspace[offset + 2] + mWorkspace[offset + 6];
			double tmp12 = (mWorkspace[offset + 2] - mWorkspace[offset + 6]) * 1.414213562 - tmp13;

			double tmp0 = tmp10 + tmp13;
			double tmp3 = tmp10 - tmp13;
			double tmp1 = tmp11 + tmp12;
			double tmp2 = tmp11 - tmp12;

			double z13 = mWorkspace[offset + 5] + mWorkspace[offset + 3];
			double z10 = mWorkspace[offset + 5] - mWorkspace[offset + 3];
			double z11 = mWorkspace[offset + 1] + mWorkspace[offset + 7];
			double z12 = mWorkspace[offset + 1] - mWorkspace[offset + 7];

			double tmp7 = z11 + z13;
			tmp11 = (z11 - z13) * 1.414213562;

			double z5 = (z10 + z12) * 1.847759065;
			tmp10 = 1.082392200 * z12 - z5;
			tmp12 = -2.613125930 * z10 + z5;

			double tmp6 = tmp12 - tmp7;
			double tmp5 = tmp11 - tmp6;
			double tmp4 = tmp10 + tmp5;

			// Final output stage: scale down by a factor of 8
			aCoefficients[offset + 0] = ((int)(tmp0 + tmp7) >> 3) + 128;
			aCoefficients[offset + 1] = ((int)(tmp1 + tmp6) >> 3) + 128;
			aCoefficients[offset + 2] = ((int)(tmp2 + tmp5) >> 3) + 128;
			aCoefficients[offset + 3] = ((int)(tmp3 - tmp4) >> 3) + 128;
			aCoefficients[offset + 4] = ((int)(tmp3 + tmp4) >> 3) + 128;
			aCoefficients[offset + 5] = ((int)(tmp2 - tmp5) >> 3) + 128;
			aCoefficients[offset + 6] = ((int)(tmp1 - tmp6) >> 3) + 128;
			aCoefficients[offset + 7] = ((int)(tmp0 - tmp7) >> 3) + 128;
		}
	}
}
