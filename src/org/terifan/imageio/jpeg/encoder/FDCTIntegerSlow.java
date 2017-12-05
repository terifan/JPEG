package org.terifan.imageio.jpeg.encoder;


public class FDCTIntegerSlow
{
	private final static int CENTERJSAMPLE = 128;
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


	public void forward(int[] aCoefficients)
	{
		int[] workspace = new int[64];

		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			int tmp0 = aCoefficients[ctr + 0] + aCoefficients[ctr + 7];
			int tmp1 = aCoefficients[ctr + 1] + aCoefficients[ctr + 6];
			int tmp2 = aCoefficients[ctr + 2] + aCoefficients[ctr + 5];
			int tmp3 = aCoefficients[ctr + 3] + aCoefficients[ctr + 4];

			int tmp10 = tmp0 + tmp3;
			int tmp12 = tmp0 - tmp3;
			int tmp11 = tmp1 + tmp2;
			int tmp13 = tmp1 - tmp2;

			tmp0 = aCoefficients[ctr + 0] - aCoefficients[ctr + 7];
			tmp1 = aCoefficients[ctr + 1] - aCoefficients[ctr + 6];
			tmp2 = aCoefficients[ctr + 2] - aCoefficients[ctr + 5];
			tmp3 = aCoefficients[ctr + 3] - aCoefficients[ctr + 4];

			workspace[ctr + 0] = ((tmp10 + tmp11 - 8 * CENTERJSAMPLE) << PASS1_BITS);
			workspace[ctr + 4] = ((tmp10 - tmp11) << PASS1_BITS);

			int z1 = mul(tmp12 + tmp13, FIX_0_541196100);
			z1 += 1 << (CONST_BITS - PASS1_BITS - 1);
			workspace[ctr + 2] = shift(z1 + mul(tmp12, FIX_0_765366865), CONST_BITS - PASS1_BITS);
			workspace[ctr + 6] = shift(z1 - mul(tmp13, FIX_1_847759065), CONST_BITS - PASS1_BITS);

			tmp10 = tmp0 + tmp3;
			tmp11 = tmp1 + tmp2;
			tmp12 = tmp0 + tmp2;
			tmp13 = tmp1 + tmp3;
			z1 = mul(tmp12 + tmp13, FIX_1_175875602);
			z1 += 1 << (CONST_BITS - PASS1_BITS - 1);

			tmp0 = mul(tmp0, FIX_1_501321110);
			tmp1 = mul(tmp1, FIX_3_072711026);
			tmp2 = mul(tmp2, FIX_2_053119869);
			tmp3 = mul(tmp3, FIX_0_298631336);
			tmp10 = mul(tmp10, -FIX_0_899976223);
			tmp11 = mul(tmp11, -FIX_2_562915447);
			tmp12 = mul(tmp12, -FIX_0_390180644);
			tmp13 = mul(tmp13, -FIX_1_961570560);

			tmp12 += z1;
			tmp13 += z1;

			workspace[ctr + 1] = shift(tmp0 + tmp10 + tmp12, CONST_BITS - PASS1_BITS);
			workspace[ctr + 3] = shift(tmp1 + tmp11 + tmp13, CONST_BITS - PASS1_BITS);
			workspace[ctr + 5] = shift(tmp2 + tmp11 + tmp12, CONST_BITS - PASS1_BITS);
			workspace[ctr + 7] = shift(tmp3 + tmp10 + tmp13, CONST_BITS - PASS1_BITS);
		}

		for (int ctr = 0; ctr < 8; ctr++)
		{
			int tmp0 = workspace[8 * 0 + ctr] + workspace[8 * 7 + ctr];
			int tmp1 = workspace[8 * 1 + ctr] + workspace[8 * 6 + ctr];
			int tmp2 = workspace[8 * 2 + ctr] + workspace[8 * 5 + ctr];
			int tmp3 = workspace[8 * 3 + ctr] + workspace[8 * 4 + ctr];

			int tmp10 = tmp0 + tmp3 + (1 << (PASS1_BITS - 1));
			int tmp12 = tmp0 - tmp3;
			int tmp11 = tmp1 + tmp2;
			int tmp13 = tmp1 - tmp2;

			tmp0 = workspace[8 * 0 + ctr] - workspace[8 * 7 + ctr];
			tmp1 = workspace[8 * 1 + ctr] - workspace[8 * 6 + ctr];
			tmp2 = workspace[8 * 2 + ctr] - workspace[8 * 5 + ctr];
			tmp3 = workspace[8 * 3 + ctr] - workspace[8 * 4 + ctr];

			aCoefficients[8 * 0 + ctr] = clamp(tmp10 + tmp11, PASS1_BITS);
			aCoefficients[8 * 4 + ctr] = clamp(tmp10 - tmp11, PASS1_BITS);

			int z1 = mul(tmp12 + tmp13, FIX_0_541196100);
			z1 += 1 << (CONST_BITS + PASS1_BITS - 1);
			aCoefficients[8 * 2 + ctr] = clamp(z1 + mul(tmp12, FIX_0_765366865), CONST_BITS + PASS1_BITS);
			aCoefficients[8 * 6 + ctr] = clamp(z1 - mul(tmp13, FIX_1_847759065), CONST_BITS + PASS1_BITS);

			tmp10 = tmp0 + tmp3;
			tmp11 = tmp1 + tmp2;
			tmp12 = tmp0 + tmp2;
			tmp13 = tmp1 + tmp3;
			z1 = mul(tmp12 + tmp13, FIX_1_175875602);
			z1 += 1 << (CONST_BITS + PASS1_BITS - 1);

			tmp0 = mul(tmp0, FIX_1_501321110);
			tmp1 = mul(tmp1, FIX_3_072711026);
			tmp2 = mul(tmp2, FIX_2_053119869);
			tmp3 = mul(tmp3, FIX_0_298631336);
			tmp10 = mul(tmp10, -FIX_0_899976223);
			tmp11 = mul(tmp11, -FIX_2_562915447);
			tmp12 = mul(tmp12, -FIX_0_390180644);
			tmp13 = mul(tmp13, -FIX_1_961570560);

			tmp12 += z1;
			tmp13 += z1;

			aCoefficients[8 * 1 + ctr] = clamp(tmp0 + tmp10 + tmp12, CONST_BITS + PASS1_BITS);
			aCoefficients[8 * 3 + ctr] = clamp(tmp1 + tmp11 + tmp13, CONST_BITS + PASS1_BITS);
			aCoefficients[8 * 5 + ctr] = clamp(tmp2 + tmp11 + tmp12, CONST_BITS + PASS1_BITS);
			aCoefficients[8 * 7 + ctr] = clamp(tmp3 + tmp10 + tmp13, CONST_BITS + PASS1_BITS);
		}
	}


	private static int clamp(int v, int q)
	{
		return v >> (q - 2);
	}


	private final static int mul(int v, int q)
	{
		return v * q;
	}


	private final static int shift(int v, int q)
	{
		return v >> q;
	}
}
