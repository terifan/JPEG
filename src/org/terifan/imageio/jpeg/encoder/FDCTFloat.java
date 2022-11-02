package org.terifan.imageio.jpeg.encoder;

import org.terifan.imageio.jpeg.QuantizationTable;



/*
 * jfdctflt.c
 *
 * Copyright (C) 1994-1996, Thomas G. Lane.
 * Modified 2003-2009 by Guido Vollbeding.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains a floating-point implementation of the
 * forward DCT (Discrete Cosine Transform).
 *
 * This implementation should be more accurate than either of the integer
 * DCT implementations.  However, it may not give the same results on all
 * machines because of differences in roundoff behavior.  Speed will depend
 * on the hardware's floating point capacity.
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
 * The primary disadvantage of this method is that with a fixed-point
 * implementation, accuracy is lost due to imprecise representation of the
 * scaled quantization values.  However, that problem does not arise if
 * we use floating point arithmetic.
 */
public class FDCTFloat implements FDCT
{
	private final static double[] AANSCALEFACTORS =
	{
	  1.0, 1.387039845, 1.306562965, 1.175875602,
	  1.0, 0.785694958, 0.541196100, 0.275899379
	};


	@Override
	public void transform(int[] aCoefficients, QuantizationTable aQuantizationTable)
	{
		double[] workspace = new double[64];

		for (int i = 0; i < 64; i++)
		{
			workspace[i] = aCoefficients[i];
		}

		transform(workspace);

		int[] quantval = aQuantizationTable.getDivisors();

		for (int row = 0, i = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++, i++)
			{
				aCoefficients[i] = (int)(workspace[i] / (quantval[i] * AANSCALEFACTORS[row] * AANSCALEFACTORS[col] * 8 / 256) + 16384.5) - 16384;
			}
		}
	}


	private void transform(double[] aCoefficients)
	{
		double[] workspace = new double[64];

		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			double tmp0 = aCoefficients[0 + ctr] + aCoefficients[7 + ctr];
			double tmp7 = aCoefficients[0 + ctr] - aCoefficients[7 + ctr];
			double tmp1 = aCoefficients[1 + ctr] + aCoefficients[6 + ctr];
			double tmp6 = aCoefficients[1 + ctr] - aCoefficients[6 + ctr];
			double tmp2 = aCoefficients[2 + ctr] + aCoefficients[5 + ctr];
			double tmp5 = aCoefficients[2 + ctr] - aCoefficients[5 + ctr];
			double tmp3 = aCoefficients[3 + ctr] + aCoefficients[4 + ctr];
			double tmp4 = aCoefficients[3 + ctr] - aCoefficients[4 + ctr];

			double tmp10 = tmp0 + tmp3;
			double tmp13 = tmp0 - tmp3;
			double tmp11 = tmp1 + tmp2;
			double tmp12 = tmp1 - tmp2;

			workspace[0 + ctr] = tmp10 + tmp11 - 8 * 128;
			workspace[4 + ctr] = tmp10 - tmp11;

			double z1 = (tmp12 + tmp13) * 0.707106781;
			workspace[2 + ctr] = tmp13 + z1;
			workspace[6 + ctr] = tmp13 - z1;

			tmp10 = tmp4 + tmp5;
			tmp11 = tmp5 + tmp6;
			tmp12 = tmp6 + tmp7;

			double z5 = (tmp10 - tmp12) * 0.382683433;
			double z2 = 0.541196100 * tmp10 + z5;
			double z4 = 1.306562965 * tmp12 + z5;
			double z3 = tmp11 * 0.707106781;

			double z11 = tmp7 + z3;
			double z13 = tmp7 - z3;

			workspace[5 + ctr] = z13 + z2;
			workspace[3 + ctr] = z13 - z2;
			workspace[1 + ctr] = z11 + z4;
			workspace[7 + ctr] = z11 - z4;
		}

		for (int ctr = 0; ctr < 8; ctr++)
		{
			double tmp0 = workspace[8 * 0 + ctr] + workspace[8 * 7 + ctr];
			double tmp7 = workspace[8 * 0 + ctr] - workspace[8 * 7 + ctr];
			double tmp1 = workspace[8 * 1 + ctr] + workspace[8 * 6 + ctr];
			double tmp6 = workspace[8 * 1 + ctr] - workspace[8 * 6 + ctr];
			double tmp2 = workspace[8 * 2 + ctr] + workspace[8 * 5 + ctr];
			double tmp5 = workspace[8 * 2 + ctr] - workspace[8 * 5 + ctr];
			double tmp3 = workspace[8 * 3 + ctr] + workspace[8 * 4 + ctr];
			double tmp4 = workspace[8 * 3 + ctr] - workspace[8 * 4 + ctr];

			double tmp10 = tmp0 + tmp3;
			double tmp13 = tmp0 - tmp3;
			double tmp11 = tmp1 + tmp2;
			double tmp12 = tmp1 - tmp2;

			aCoefficients[8 * 0 + ctr] = tmp10 + tmp11;
			aCoefficients[8 * 4 + ctr] = tmp10 - tmp11;

			double z1 = (tmp12 + tmp13) * 0.707106781;
			aCoefficients[8 * 2 + ctr] = tmp13 + z1;
			aCoefficients[8 * 6 + ctr] = tmp13 - z1;

			tmp10 = tmp4 + tmp5;
			tmp11 = tmp5 + tmp6;
			tmp12 = tmp6 + tmp7;

			double z5 = (tmp10 - tmp12) * 0.382683433;
			double z2 = 0.541196100 * tmp10 + z5;
			double z4 = 1.306562965 * tmp12 + z5;
			double z3 = tmp11 * 0.707106781;

			double z11 = tmp7 + z3;
			double z13 = tmp7 - z3;

			aCoefficients[8 * 5 + ctr] = z13 + z2;
			aCoefficients[8 * 3 + ctr] = z13 - z2;
			aCoefficients[8 * 1 + ctr] = z11 + z4;
			aCoefficients[8 * 7 + ctr] = z11 - z4;
		}
	}
}
