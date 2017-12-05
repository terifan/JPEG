package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.DQTMarkerSegment;


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
	/**
	 * Perform dequantization and inverse DCT on one block of coefficients.
	 */
	@Override
	public void transform(int[] aCoefficients, DQTMarkerSegment aQuantizationTable)
	{
		int[] mWorkspace = new int[64];
		int[] quantizationTable = aQuantizationTable.getTableInt();

		// Pass 1: process columns from input, store into work array.
		for (int ctr = 0; ctr < 8; ctr++)
		{
			if (aCoefficients[8 + ctr] == 0 && aCoefficients[16 + ctr] == 0 && aCoefficients[24 + ctr] == 0 && aCoefficients[32 + ctr] == 0 && aCoefficients[40 + ctr] == 0 && aCoefficients[48 + ctr] == 0 && aCoefficients[56 + ctr] == 0)
			{
				// AC terms all zero
				int dcval = aCoefficients[ctr] * quantizationTable[ctr];

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

			int tmp0 = aCoefficients[ctr] * quantizationTable[ctr];
			int tmp1 = aCoefficients[16 + ctr] * quantizationTable[16 + ctr];
			int tmp2 = aCoefficients[32 + ctr] * quantizationTable[32 + ctr];
			int tmp3 = aCoefficients[48 + ctr] * quantizationTable[48 + ctr];

			int tmp10 = tmp0 + tmp2;
			int tmp11 = tmp0 - tmp2;

			int tmp13 = tmp1 + tmp3;
			int tmp12 = (((tmp1 - tmp3) * 362) >> 8) - tmp13;

			tmp0 = tmp10 + tmp13;
			tmp3 = tmp10 - tmp13;
			tmp1 = tmp11 + tmp12;
			tmp2 = tmp11 - tmp12;

			int tmp4 = aCoefficients[8 + ctr] * quantizationTable[8 + ctr];
			int tmp5 = aCoefficients[24 + ctr] * quantizationTable[24 + ctr];
			int tmp6 = aCoefficients[40 + ctr] * quantizationTable[40 + ctr];
			int tmp7 = aCoefficients[56 + ctr] * quantizationTable[56 + ctr];

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
		for (int ctr = 0; ctr < 8; ctr++)
		{
			int offset = ctr * 8;

			if (mWorkspace[offset + 1] == 0 && mWorkspace[offset + 2] == 0 && mWorkspace[offset + 3] == 0 && mWorkspace[offset + 4] == 0 && mWorkspace[offset + 5] == 0 && mWorkspace[offset + 6] == 0 && mWorkspace[offset + 7] == 0)
			{
				// AC terms all zero
				int dcval = clamp(mWorkspace[offset]);

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

			int tmp10 = mWorkspace[offset] + mWorkspace[offset + 4];
			int tmp11 = mWorkspace[offset] - mWorkspace[offset + 4];

			int tmp13 = mWorkspace[offset + 2] + mWorkspace[offset + 6];
			int tmp12 = (((mWorkspace[offset + 2] - mWorkspace[offset + 6]) * 362) >> 8) - tmp13;

			int tmp0 = tmp10 + tmp13;
			int tmp3 = tmp10 - tmp13;
			int tmp1 = tmp11 + tmp12;
			int tmp2 = tmp11 - tmp12;

			int z13 = mWorkspace[offset + 5] + mWorkspace[offset + 3];
			int z10 = mWorkspace[offset + 5] - mWorkspace[offset + 3];
			int z11 = mWorkspace[offset + 1] + mWorkspace[offset + 7];
			int z12 = mWorkspace[offset + 1] - mWorkspace[offset + 7];

			int tmp7 = z11 + z13;
			tmp11 = ((z11 - z13) * 362) >> 8;

			int z5 = ((z10 + z12) * 473) >> 8;
			tmp10 = ((z12 * 277) >> 8) - z5;
			tmp12 = ((z10 * -669) >> 8) + z5;

			int tmp6 = tmp12 - tmp7;
			int tmp5 = tmp11 - tmp6;
			int tmp4 = tmp10 + tmp5;

			// Final output stage: scale down by a factor of 8
			aCoefficients[offset + 0] = clamp(tmp0 + tmp7);
			aCoefficients[offset + 1] = clamp(tmp1 + tmp6);
			aCoefficients[offset + 2] = clamp(tmp2 + tmp5);
			aCoefficients[offset + 3] = clamp(tmp3 - tmp4);
			aCoefficients[offset + 4] = clamp(tmp3 + tmp4);
			aCoefficients[offset + 5] = clamp(tmp2 - tmp5);
			aCoefficients[offset + 6] = clamp(tmp1 - tmp6);
			aCoefficients[offset + 7] = clamp(tmp0 - tmp7);
		}
	}


	public void transform(int[] aCoefficients)
	{
		int[] mWorkspace = new int[64];

		// Pass 1: process columns from input, store into work array.
		for (int ctr = 0; ctr < 8; ctr++)
		{
			if (aCoefficients[8 + ctr] == 0 && aCoefficients[16 + ctr] == 0 && aCoefficients[24 + ctr] == 0 && aCoefficients[32 + ctr] == 0 && aCoefficients[40 + ctr] == 0 && aCoefficients[48 + ctr] == 0 && aCoefficients[56 + ctr] == 0)
			{
				// AC terms all zero
				int dcval = aCoefficients[ctr];

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

			int tmp0 = aCoefficients[ctr];
			int tmp1 = aCoefficients[16 + ctr];
			int tmp2 = aCoefficients[32 + ctr];
			int tmp3 = aCoefficients[48 + ctr];

			int tmp10 = tmp0 + tmp2;
			int tmp11 = tmp0 - tmp2;

			int tmp13 = tmp1 + tmp3;
			int tmp12 = (((tmp1 - tmp3) * 362) >> 8) - tmp13;

			tmp0 = tmp10 + tmp13;
			tmp3 = tmp10 - tmp13;
			tmp1 = tmp11 + tmp12;
			tmp2 = tmp11 - tmp12;

			int tmp4 = aCoefficients[8 + ctr];
			int tmp5 = aCoefficients[24 + ctr];
			int tmp6 = aCoefficients[40 + ctr];
			int tmp7 = aCoefficients[56 + ctr];

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
		for (int ctr = 0; ctr < 8; ctr++)
		{
			int offset = ctr * 8;

			if (mWorkspace[offset + 1] == 0 && mWorkspace[offset + 2] == 0 && mWorkspace[offset + 3] == 0 && mWorkspace[offset + 4] == 0 && mWorkspace[offset + 5] == 0 && mWorkspace[offset + 6] == 0 && mWorkspace[offset + 7] == 0)
			{
				// AC terms all zero
				int dcval = clamp(mWorkspace[offset]);

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

			int tmp10 = mWorkspace[offset] + mWorkspace[offset + 4];
			int tmp11 = mWorkspace[offset] - mWorkspace[offset + 4];

			int tmp13 = mWorkspace[offset + 2] + mWorkspace[offset + 6];
			int tmp12 = (((mWorkspace[offset + 2] - mWorkspace[offset + 6]) * 362) >> 8) - tmp13;

			int tmp0 = tmp10 + tmp13;
			int tmp3 = tmp10 - tmp13;
			int tmp1 = tmp11 + tmp12;
			int tmp2 = tmp11 - tmp12;

			int z13 = mWorkspace[offset + 5] + mWorkspace[offset + 3];
			int z10 = mWorkspace[offset + 5] - mWorkspace[offset + 3];
			int z11 = mWorkspace[offset + 1] + mWorkspace[offset + 7];
			int z12 = mWorkspace[offset + 1] - mWorkspace[offset + 7];

			int tmp7 = z11 + z13;
			tmp11 = ((z11 - z13) * 362) >> 8;

			int z5 = ((z10 + z12) * 473) >> 8;
			tmp10 = ((z12 * 277) >> 8) - z5;
			tmp12 = ((z10 * -669) >> 8) + z5;

			int tmp6 = tmp12 - tmp7;
			int tmp5 = tmp11 - tmp6;
			int tmp4 = tmp10 + tmp5;

			// Final output stage: scale down by a factor of 8
			aCoefficients[offset + 0] = clamp(tmp0 + tmp7);
			aCoefficients[offset + 1] = clamp(tmp1 + tmp6);
			aCoefficients[offset + 2] = clamp(tmp2 + tmp5);
			aCoefficients[offset + 3] = clamp(tmp3 - tmp4);
			aCoefficients[offset + 4] = clamp(tmp3 + tmp4);
			aCoefficients[offset + 5] = clamp(tmp2 - tmp5);
			aCoefficients[offset + 6] = clamp(tmp1 - tmp6);
			aCoefficients[offset + 7] = clamp(tmp0 - tmp7);
		}
	}


	private static int clamp(int aValue)
	{
		aValue = 128 + ((aValue + 128) >> 8);

		return aValue < 0 ? 0 : aValue > 255 ? 255 : aValue;
	}
}
