package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.DQTMarkerSegment;


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
public class IDCTFloat implements IDCT
{
	private final static double[] AANSCALEFACTORS =
	{
		1.0, 1.387039845, 1.306562965, 1.175875602,
		1.0, 0.785694958, 0.541196100, 0.275899379
	};


	@Override
	public void transform(int[] aCoefficients, DQTMarkerSegment aQuantizationTable)
	{
		double[] quantval = aQuantizationTable.getFloatDivisors();

		for (int row = 0, i = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++, i++)
			{
				aCoefficients[i] *= quantval[i] * AANSCALEFACTORS[row] * AANSCALEFACTORS[col] * 0.125;
			}
		}

		transform(aCoefficients);
	}


	@Override
	public void transform(int[] aCoefficients)
	{
		double[] workspace = new double[64];

		// Pass 1: process columns from input, store into work array.
		for (int ctr = 0; ctr < 8; ctr++)
		{
			if (aCoefficients[8 + ctr] == 0 && aCoefficients[16 + ctr] == 0 && aCoefficients[24 + ctr] == 0 && aCoefficients[32 + ctr] == 0 && aCoefficients[40 + ctr] == 0 && aCoefficients[48 + ctr] == 0 && aCoefficients[56 + ctr] == 0)
			{
				// AC terms all zero
				double dcval = aCoefficients[ctr];

				workspace[8 * 0 + ctr] = dcval;
				workspace[8 * 1 + ctr] = dcval;
				workspace[8 * 2 + ctr] = dcval;
				workspace[8 * 3 + ctr] = dcval;
				workspace[8 * 4 + ctr] = dcval;
				workspace[8 * 5 + ctr] = dcval;
				workspace[8 * 6 + ctr] = dcval;
				workspace[8 * 7 + ctr] = dcval;

				continue;
			}

			double tmp0 = aCoefficients[8 * 0 + ctr];
			double tmp1 = aCoefficients[8 * 2 + ctr];
			double tmp2 = aCoefficients[8 * 4 + ctr];
			double tmp3 = aCoefficients[8 * 6 + ctr];

			double tmp10 = tmp0 + tmp2;
			double tmp11 = tmp0 - tmp2;

			double tmp13 = tmp1 + tmp3;
			double tmp12 = (tmp1 - tmp3) * 1.414213562 - tmp13;

			tmp0 = tmp10 + tmp13;
			tmp3 = tmp10 - tmp13;
			tmp1 = tmp11 + tmp12;
			tmp2 = tmp11 - tmp12;

			double tmp4 = aCoefficients[8 * 1 + ctr];
			double tmp5 = aCoefficients[8 * 3 + ctr];
			double tmp6 = aCoefficients[8 * 5 + ctr];
			double tmp7 = aCoefficients[8 * 7 + ctr];

			double z13 = tmp6 + tmp5;
			double z10 = tmp6 - tmp5;
			double z11 = tmp4 + tmp7;
			double z12 = tmp4 - tmp7;

			tmp7 = z11 + z13;
			tmp11 = (z11 - z13) * 1.414213562;

			double z5 = (z10 + z12) * 1.847759065;
			tmp10 = z5 - z12 * 1.0823922;
			tmp12 = z5 - z10 * 2.61312593;

			tmp6 = tmp12 - tmp7;
			tmp5 = tmp11 - tmp6;
			tmp4 = tmp10 - tmp5;

			workspace[8 * 0 + ctr] = tmp0 + tmp7;
			workspace[8 * 7 + ctr] = tmp0 - tmp7;
			workspace[8 * 1 + ctr] = tmp1 + tmp6;
			workspace[8 * 6 + ctr] = tmp1 - tmp6;
			workspace[8 * 2 + ctr] = tmp2 + tmp5;
			workspace[8 * 5 + ctr] = tmp2 - tmp5;
			workspace[8 * 3 + ctr] = tmp3 + tmp4;
			workspace[8 * 4 + ctr] = tmp3 - tmp4;
		}

		// Pass 2: process rows from work array, store into output array.
		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			double z5 = workspace[ctr] + (128 + 0.5);
			double tmp10 = z5 + workspace[ctr + 4];
			double tmp11 = z5 - workspace[ctr + 4];

			double tmp13 = workspace[ctr + 2] + workspace[ctr + 6];
			double tmp12 = (workspace[ctr + 2] - workspace[ctr + 6]) * 1.414213562 - tmp13;

			double tmp0 = tmp10 + tmp13;
			double tmp3 = tmp10 - tmp13;
			double tmp1 = tmp11 + tmp12;
			double tmp2 = tmp11 - tmp12;

			double z13 = workspace[ctr + 5] + workspace[ctr + 3];
			double z10 = workspace[ctr + 5] - workspace[ctr + 3];
			double z11 = workspace[ctr + 1] + workspace[ctr + 7];
			double z12 = workspace[ctr + 1] - workspace[ctr + 7];

			double tmp7 = z11 + z13;
			tmp11 = (z11 - z13) * 1.414213562;

			z5 = (z10 + z12) * 1.847759065;
			tmp10 = z5 - z12 * 1.082392200;
			tmp12 = z5 - z10 * 2.613125930;

			double tmp6 = tmp12 - tmp7;
			double tmp5 = tmp11 - tmp6;
			double tmp4 = tmp10 - tmp5;

			// Final output stage: scale down by a factor of 8
			aCoefficients[ctr + 0] = clamp(tmp0 + tmp7);
			aCoefficients[ctr + 7] = clamp(tmp0 - tmp7);
			aCoefficients[ctr + 1] = clamp(tmp1 + tmp6);
			aCoefficients[ctr + 6] = clamp(tmp1 - tmp6);
			aCoefficients[ctr + 2] = clamp(tmp2 + tmp5);
			aCoefficients[ctr + 5] = clamp(tmp2 - tmp5);
			aCoefficients[ctr + 3] = clamp(tmp3 + tmp4);
			aCoefficients[ctr + 4] = clamp(tmp3 - tmp4);
		}
	}


	private static int clamp(double aValue)
	{
		return (int)(aValue);
	}
}
