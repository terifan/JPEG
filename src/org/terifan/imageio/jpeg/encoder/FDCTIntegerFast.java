package org.terifan.imageio.jpeg.encoder;


public class FDCTIntegerFast
{
	private final static int CENTERJSAMPLE = 128;
	private final static int CONST_BITS = 8;
	private final static int FIX_0_382683433 = 98;
	private final static int FIX_0_541196100 = 139;
	private final static int FIX_0_707106781 = 181;
	private final static int FIX_1_306562965 = 334;


	public void transform(int[] aCoefficients)
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

			aCoefficients[ctr + 8 * 0] = (tmp10 + tmp11)<<2;
			aCoefficients[ctr + 8 * 4] = (tmp10 - tmp11)<<2;

			int z1 = MULTIPLY(tmp12 + tmp13, FIX_0_707106781);
			aCoefficients[ctr + 8 * 2] = (tmp13 + z1)<<2;
			aCoefficients[ctr + 8 * 6] = (tmp13 - z1)<<2;

			tmp10 = tmp4 + tmp5;
			tmp11 = tmp5 + tmp6;
			tmp12 = tmp6 + tmp7;

			int z5 = MULTIPLY(tmp10 - tmp12, FIX_0_382683433);
			int z2 = MULTIPLY(tmp10, FIX_0_541196100) + z5;
			int z4 = MULTIPLY(tmp12, FIX_1_306562965) + z5;
			int z3 = MULTIPLY(tmp11, FIX_0_707106781);

			int z11 = tmp7 + z3;
			int z13 = tmp7 - z3;

			aCoefficients[ctr + 8 * 5] = (z13 + z2)<<2;
			aCoefficients[ctr + 8 * 3] = (z13 - z2)<<2;
			aCoefficients[ctr + 8 * 1] = (z11 + z4)<<2;
			aCoefficients[ctr + 8 * 7] = (z11 - z4)<<2;
		}
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}


	private static int MULTIPLY(int x, int n)
	{
//		return DESCALE(x * n, CONST_BITS);
		return (x * n) >> CONST_BITS;
	}
}
