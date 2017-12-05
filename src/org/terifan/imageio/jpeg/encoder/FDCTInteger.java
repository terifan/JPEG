package org.terifan.imageio.jpeg.encoder;


public class FDCTInteger
{
	private final static int CONST_BITS = 13;
	private final static int PASS1_BITS = 1;
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

	private final static int W1 = 2841;
	private final static int W2 = 2676;
	private final static int W3 = 2408;
	private final static int W5 = 1609;
	private final static int W6 = 1108;
	private final static int W7 = 565;

	private final static int[] CLIP = new int[32768];


	static
	{
		for (int i = -16384; i < 16384; i++)
		{
			CLIP[16384 + i] = (i < 0) ? 0 : ((i > 16384 + 255) ? 255 : i);
		}
	}


	public void forward(int[] aBlock)
	{
		int[] workspace = new int[64];

		for (int ctr = 0; ctr < 64; ctr += 8)
		{
			int tmp0 = aBlock[ctr + 0] + aBlock[ctr + 7];
			int tmp7 = aBlock[ctr + 0] - aBlock[ctr + 7];
			int tmp1 = aBlock[ctr + 1] + aBlock[ctr + 6];
			int tmp6 = aBlock[ctr + 1] - aBlock[ctr + 6];
			int tmp2 = aBlock[ctr + 2] + aBlock[ctr + 5];
			int tmp5 = aBlock[ctr + 2] - aBlock[ctr + 5];
			int tmp3 = aBlock[ctr + 3] + aBlock[ctr + 4];
			int tmp4 = aBlock[ctr + 3] - aBlock[ctr + 4];

			int tmp10 = tmp0 + tmp3;
			int tmp13 = tmp0 - tmp3;
			int tmp11 = tmp1 + tmp2;
			int tmp12 = tmp1 - tmp2;

			workspace[ctr + 0] = (tmp10 + tmp11) << PASS1_BITS;
			workspace[ctr + 4] = (tmp10 - tmp11) << PASS1_BITS;

			int z1 = (tmp12 + tmp13) * FIX_0_541196100;
			workspace[ctr + 2] = DESCALE(z1 + tmp13 * FIX_0_765366865, CONST_BITS - PASS1_BITS);
			workspace[ctr + 6] = DESCALE(z1 + tmp12 * (-FIX_1_847759065), CONST_BITS - PASS1_BITS);

			int z6 = tmp4 + tmp7;
			int z2 = tmp5 + tmp6;
			int z3 = tmp4 + tmp6;
			int z4 = tmp5 + tmp7;
			int z5 = (z3 + z4) * FIX_1_175875602;

			tmp4 *= FIX_0_298631336;
			tmp5 *= FIX_2_053119869;
			tmp6 *= FIX_3_072711026;
			tmp7 *= FIX_1_501321110;
			z6 *= -FIX_0_899976223;
			z2 *= -FIX_2_562915447;
			z3 *= -FIX_1_961570560;
			z4 *= -FIX_0_390180644;

			z3 += z5;
			z4 += z5;

			workspace[ctr + 7] = DESCALE(tmp4 + z6 + z3, CONST_BITS - PASS1_BITS);
			workspace[ctr + 5] = DESCALE(tmp5 + z2 + z4, CONST_BITS - PASS1_BITS);
			workspace[ctr + 3] = DESCALE(tmp6 + z2 + z3, CONST_BITS - PASS1_BITS);
			workspace[ctr + 1] = DESCALE(tmp7 + z6 + z4, CONST_BITS - PASS1_BITS);
		}

		for (int ctr = 0; ctr < 8; ctr++)
		{
			int tmp0 = workspace[ctr + 0 * 8] + workspace[ctr + 7 * 8];
			int tmp7 = workspace[ctr + 0 * 8] - workspace[ctr + 7 * 8];
			int tmp1 = workspace[ctr + 1 * 8] + workspace[ctr + 6 * 8];
			int tmp6 = workspace[ctr + 1 * 8] - workspace[ctr + 6 * 8];
			int tmp2 = workspace[ctr + 2 * 8] + workspace[ctr + 5 * 8];
			int tmp5 = workspace[ctr + 2 * 8] - workspace[ctr + 5 * 8];
			int tmp3 = workspace[ctr + 3 * 8] + workspace[ctr + 4 * 8];
			int tmp4 = workspace[ctr + 3 * 8] - workspace[ctr + 4 * 8];

			int tmp10 = tmp0 + tmp3;
			int tmp13 = tmp0 - tmp3;
			int tmp11 = tmp1 + tmp2;
			int tmp12 = tmp1 - tmp2;

			workspace[ctr + 0 * 8] = DESCALE(tmp10 + tmp11, PASS1_BITS);
			workspace[ctr + 4 * 8] = DESCALE(tmp10 - tmp11, PASS1_BITS);

			int z1 = (tmp12 + tmp13) * FIX_0_541196100;
			workspace[ctr + 2 * 8] = DESCALE(z1 + tmp13 * FIX_0_765366865, CONST_BITS + PASS1_BITS);
			workspace[ctr + 6 * 8] = DESCALE(z1 + tmp12 * (-FIX_1_847759065), CONST_BITS + PASS1_BITS);

			int z6 = tmp4 + tmp7;
			int z2 = tmp5 + tmp6;
			int z3 = tmp4 + tmp6;
			int z4 = tmp5 + tmp7;
			int z5 = (z3 + z4) * FIX_1_175875602;

			tmp4 *= FIX_0_298631336;
			tmp5 *= FIX_2_053119869;
			tmp6 *= FIX_3_072711026;
			tmp7 *= FIX_1_501321110;
			z6 *= -FIX_0_899976223;
			z2 *= -FIX_2_562915447;
			z3 *= -FIX_1_961570560;
			z4 *= -FIX_0_390180644;

			z3 += z5;
			z4 += z5;

			workspace[ctr + 7 * 8] = DESCALE(tmp4 + z6 + z3, CONST_BITS + PASS1_BITS);
			workspace[ctr + 5 * 8] = DESCALE(tmp5 + z2 + z4, CONST_BITS + PASS1_BITS);
			workspace[ctr + 3 * 8] = DESCALE(tmp6 + z2 + z3, CONST_BITS + PASS1_BITS);
			workspace[ctr + 1 * 8] = DESCALE(tmp7 + z6 + z4, CONST_BITS + PASS1_BITS);
		}

		for (int i = 0; i < 64; i++)
		{
			aBlock[i] = DESCALE(workspace[i], 3);
		}
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}
}
