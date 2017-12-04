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


	public void transform(int[] block)
	{
		int[] buf = new int[64];

		for (int i = 0, blkptr = 0, dataptr = 0; i < 8; i++)
		{
			int tmp0 = block[blkptr + 0] + block[blkptr + 7];
			int tmp7 = block[blkptr + 0] - block[blkptr + 7];
			int tmp1 = block[blkptr + 1] + block[blkptr + 6];
			int tmp6 = block[blkptr + 1] - block[blkptr + 6];
			int tmp2 = block[blkptr + 2] + block[blkptr + 5];
			int tmp5 = block[blkptr + 2] - block[blkptr + 5];
			int tmp3 = block[blkptr + 3] + block[blkptr + 4];
			int tmp4 = block[blkptr + 3] - block[blkptr + 4];

			int tmp10 = tmp0 + tmp3;
			int tmp13 = tmp0 - tmp3;
			int tmp11 = tmp1 + tmp2;
			int tmp12 = tmp1 - tmp2;

			buf[dataptr + 0] = (tmp10 + tmp11) << PASS1_BITS;
			buf[dataptr + 4] = (tmp10 - tmp11) << PASS1_BITS;

			int z1 = (tmp12 + tmp13) * FIX_0_541196100;
			buf[dataptr + 2] = DESCALE(z1 + tmp13 * FIX_0_765366865, CONST_BITS - PASS1_BITS);
			buf[dataptr + 6] = DESCALE(z1 + tmp12 * (-FIX_1_847759065), CONST_BITS - PASS1_BITS);

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

			buf[dataptr + 7] = DESCALE(tmp4 + z6 + z3, CONST_BITS - PASS1_BITS);
			buf[dataptr + 5] = DESCALE(tmp5 + z2 + z4, CONST_BITS - PASS1_BITS);
			buf[dataptr + 3] = DESCALE(tmp6 + z2 + z3, CONST_BITS - PASS1_BITS);
			buf[dataptr + 1] = DESCALE(tmp7 + z6 + z4, CONST_BITS - PASS1_BITS);

			dataptr += 8;
			blkptr += 8;
		}

		for (int i = 0, dataptr = 0; i < 8; i++)
		{
			int tmp0 = buf[dataptr + 0*8] + buf[dataptr + 7*8];
			int tmp7 = buf[dataptr + 0*8] - buf[dataptr + 7*8];
			int tmp1 = buf[dataptr + 1*8] + buf[dataptr + 6*8];
			int tmp6 = buf[dataptr + 1*8] - buf[dataptr + 6*8];
			int tmp2 = buf[dataptr + 2*8] + buf[dataptr + 5*8];
			int tmp5 = buf[dataptr + 2*8] - buf[dataptr + 5*8];
			int tmp3 = buf[dataptr + 3*8] + buf[dataptr + 4*8];
			int tmp4 = buf[dataptr + 3*8] - buf[dataptr + 4*8];

			int tmp10 = tmp0 + tmp3;
			int tmp13 = tmp0 - tmp3;
			int tmp11 = tmp1 + tmp2;
			int tmp12 = tmp1 - tmp2;

			buf[dataptr + 0*8] = DESCALE(tmp10 + tmp11, PASS1_BITS);
			buf[dataptr + 4*8] = DESCALE(tmp10 - tmp11, PASS1_BITS);

			int z1 = (tmp12 + tmp13) * FIX_0_541196100;
			buf[dataptr + 2*8] = DESCALE(z1 + tmp13 * FIX_0_765366865, CONST_BITS + PASS1_BITS);
			buf[dataptr + 6*8] = DESCALE(z1 + tmp12 * (-FIX_1_847759065), CONST_BITS + PASS1_BITS);

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

			buf[dataptr + 7*8] = DESCALE(tmp4 + z6 + z3, CONST_BITS + PASS1_BITS);
			buf[dataptr + 5*8] = DESCALE(tmp5 + z2 + z4, CONST_BITS + PASS1_BITS);
			buf[dataptr + 3*8] = DESCALE(tmp6 + z2 + z3, CONST_BITS + PASS1_BITS);
			buf[dataptr + 1*8] = DESCALE(tmp7 + z6 + z4, CONST_BITS + PASS1_BITS);

			dataptr++;
		}

		for (int i = 0; i < 64; i++)
		{
			block[i] = DESCALE(buf[i], 3);
		}
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}
}
