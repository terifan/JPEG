package org.terifan.imageio.jpeg.encoder;


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
public class FDCTFloat
{
	int CENTERJSAMPLE = 128;


	void forward(int[] elemptr)
	{
		double[] dataptr = new double[64];

		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			double tmp0 = elemptr[0 + ctr] + elemptr[7 + ctr];
			double tmp7 = elemptr[0 + ctr] - elemptr[7 + ctr];
			double tmp1 = elemptr[1 + ctr] + elemptr[6 + ctr];
			double tmp6 = elemptr[1 + ctr] - elemptr[6 + ctr];
			double tmp2 = elemptr[2 + ctr] + elemptr[5 + ctr];
			double tmp5 = elemptr[2 + ctr] - elemptr[5 + ctr];
			double tmp3 = elemptr[3 + ctr] + elemptr[4 + ctr];
			double tmp4 = elemptr[3 + ctr] - elemptr[4 + ctr];

			double tmp10 = tmp0 + tmp3;
			double tmp13 = tmp0 - tmp3;
			double tmp11 = tmp1 + tmp2;
			double tmp12 = tmp1 - tmp2;

			dataptr[0+ctr] = tmp10 + tmp11 - 8 * CENTERJSAMPLE;
			dataptr[4+ctr] = tmp10 - tmp11;

			double z1 = (tmp12 + tmp13) * (0.707106781);
			dataptr[2+ctr] = tmp13 + z1;
			dataptr[6+ctr] = tmp13 - z1;

			tmp10 = tmp4 + tmp5;
			tmp11 = tmp5 + tmp6;
			tmp12 = tmp6 + tmp7;

			double z5 = (tmp10 - tmp12) * (0.382683433);
			double z2 = (0.541196100) * tmp10 + z5;
			double z4 = (1.306562965) * tmp12 + z5;
			double z3 = tmp11 * (0.707106781);

			double z11 = tmp7 + z3;
			double z13 = tmp7 - z3;

			dataptr[5+ctr] = z13 + z2;
			dataptr[3+ctr] = z13 - z2;
			dataptr[1+ctr] = z11 + z4;
			dataptr[7+ctr] = z11 - z4;
		}

		for (int ctr = 0; ctr < 8; ctr++)
		{
			double tmp0 = dataptr[8 * 0 + ctr] + dataptr[8 * 7 + ctr];
			double tmp7 = dataptr[8 * 0 + ctr] - dataptr[8 * 7 + ctr];
			double tmp1 = dataptr[8 * 1 + ctr] + dataptr[8 * 6 + ctr];
			double tmp6 = dataptr[8 * 1 + ctr] - dataptr[8 * 6 + ctr];
			double tmp2 = dataptr[8 * 2 + ctr] + dataptr[8 * 5 + ctr];
			double tmp5 = dataptr[8 * 2 + ctr] - dataptr[8 * 5 + ctr];
			double tmp3 = dataptr[8 * 3 + ctr] + dataptr[8 * 4 + ctr];
			double tmp4 = dataptr[8 * 3 + ctr] - dataptr[8 * 4 + ctr];

			double tmp10 = tmp0 + tmp3;
			double tmp13 = tmp0 - tmp3;
			double tmp11 = tmp1 + tmp2;
			double tmp12 = tmp1 - tmp2;

			dataptr[8 * 0 + ctr] = tmp10 + tmp11;
			dataptr[8 * 4 + ctr] = tmp10 - tmp11;

			double z1 = (tmp12 + tmp13) * (0.707106781);
			dataptr[8 * 2 + ctr] = tmp13 + z1;
			dataptr[8 * 6 + ctr] = tmp13 - z1;

			tmp10 = tmp4 + tmp5;
			tmp11 = tmp5 + tmp6;
			tmp12 = tmp6 + tmp7;

			double z5 = (tmp10 - tmp12) * (0.382683433);
			double z2 = (0.541196100) * tmp10 + z5;
			double z4 = (1.306562965) * tmp12 + z5;
			double z3 = tmp11 * (0.707106781);

			double z11 = tmp7 + z3;
			double z13 = tmp7 - z3;

			dataptr[8 * 5 + ctr] = z13 + z2;
			dataptr[8 * 3 + ctr] = z13 - z2;
			dataptr[8 * 1 + ctr] = z11 + z4;
			dataptr[8 * 7 + ctr] = z11 - z4;
		}
		
		for (int i = 0; i < 64; i++)
		{
			elemptr[i] = (int)dataptr[i];
		}
	}
}