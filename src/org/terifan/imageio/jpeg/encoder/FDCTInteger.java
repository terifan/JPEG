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
			CLIP[16384 + i] = (i < 0) ? 0 : ((i > 16384+255) ? 255 : i);
		}
	}

	public void forward(int[] block)
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


	public void inverse(int[] block)
	{
		for (int i = 0; i < 8; i++)
		{
			int blk = 8 * i;

			int tmp0;
			int tmp1 = block[blk + 4] << 11;
			int tmp2 = block[blk + 6];
			int tmp3 = block[blk + 2];
			int tmp4 = block[blk + 1];
			int tmp5 = block[blk + 7];
			int tmp6 = block[blk + 5];
			int tmp7 = block[blk + 3];
			int tmp8;

			if (tmp1 == 0 && tmp2 == 0 && tmp3 == 0 && tmp4 == 0 && tmp5 == 0 && tmp6 == 0 && tmp7 == 0)
			{
				block[blk + 0] = block[blk + 1] = block[blk + 2] = block[blk + 3] = block[blk + 4] = block[blk + 5] = block[blk + 6] = block[blk + 7] = block[blk + 0] << 3;
				continue;
			}

			tmp0 = (block[blk + 0] << 11) + 128;

			// first stage
			tmp8 = W7 * (tmp4 + tmp5);
			tmp4 = tmp8 + (W1 - W7) * tmp4;
			tmp5 = tmp8 - (W1 + W7) * tmp5;
			tmp8 = W3 * (tmp6 + tmp7);
			tmp6 = tmp8 - (W3 - W5) * tmp6;
			tmp7 = tmp8 - (W3 + W5) * tmp7;

			// second stage
			tmp8 = tmp0 + tmp1;
			tmp0 -= tmp1;
			tmp1 = W6 * (tmp3 + tmp2);
			tmp2 = tmp1 - (W2 + W6) * tmp2;
			tmp3 = tmp1 + (W2 - W6) * tmp3;
			tmp1 = tmp4 + tmp6;
			tmp4 -= tmp6;
			tmp6 = tmp5 + tmp7;
			tmp5 -= tmp7;

			// third stage
			tmp7 = tmp8 + tmp3;
			tmp8 -= tmp3;
			tmp3 = tmp0 + tmp2;
			tmp0 -= tmp2;
			tmp2 = (181 * (tmp4 + tmp5) + 128) >> 8;
			tmp4 = (181 * (tmp4 - tmp5) + 128) >> 8;

			// fourth stage

			block[blk + 0] = ((tmp7 + tmp1) >> 8);
			block[blk + 1] = ((tmp3 + tmp2) >> 8);
			block[blk + 2] = ((tmp0 + tmp4) >> 8);
			block[blk + 3] = ((tmp8 + tmp6) >> 8);
			block[blk + 4] = ((tmp8 - tmp6) >> 8);
			block[blk + 5] = ((tmp0 - tmp4) >> 8);
			block[blk + 6] = ((tmp3 - tmp2) >> 8);
			block[blk + 7] = ((tmp7 - tmp1) >> 8);
		}

		for (int i = 0; i < 8; i++)
		{
			int blk = i;

			int tmp0;
			int tmp1 = block[blk + 8 * 4] << 8;
			int tmp2 = block[blk + 8 * 6];
			int tmp3 = block[blk + 8 * 2];
			int tmp4 = block[blk + 8 * 1];
			int tmp5 = block[blk + 8 * 7];
			int tmp6 = block[blk + 8 * 5];
			int tmp7 = block[blk + 8 * 3];
			int tmp8;

			if (tmp1 == 0 && tmp2 == 0 && tmp3 == 0 && tmp4 == 0 && tmp5 == 0 && tmp6 == 0 && tmp7 == 0)
			{
				block[blk + 8 * 0] = block[blk + 8 * 1] = block[blk + 8 * 2] = block[blk + 8 * 3] = block[blk + 8 * 4] = block[blk + 8 * 5] = block[blk + 8 * 6] = block[blk + 8 * 7] = CLIP[16384 + ((block[blk + 8 * 0] + 32) >> 6)];
				continue;
			}

			tmp0 = (block[blk + 8 * 0] << 8) + 8192;

			// first stage
			tmp8 = W7 * (tmp4 + tmp5) + 4;
			tmp4 = (tmp8 + (W1 - W7) * tmp4) >> 3;
			tmp5 = (tmp8 - (W1 + W7) * tmp5) >> 3;
			tmp8 = W3 * (tmp6 + tmp7) + 4;
			tmp6 = (tmp8 - (W3 - W5) * tmp6) >> 3;
			tmp7 = (tmp8 - (W3 + W5) * tmp7) >> 3;

			// second stage
			tmp8 = tmp0 + tmp1;
			tmp0 -= tmp1;
			tmp1 = W6 * (tmp3 + tmp2) + 4;
			tmp2 = (tmp1 - (W2 + W6) * tmp2) >> 3;
			tmp3 = (tmp1 + (W2 - W6) * tmp3) >> 3;
			tmp1 = tmp4 + tmp6;
			tmp4 -= tmp6;
			tmp6 = tmp5 + tmp7;
			tmp5 -= tmp7;

			// third stage
			tmp7 = tmp8 + tmp3;
			tmp8 -= tmp3;
			tmp3 = tmp0 + tmp2;
			tmp0 -= tmp2;
			tmp2 = (181 * (tmp4 + tmp5) + 128) >> 8;
			tmp4 = (181 * (tmp4 - tmp5) + 128) >> 8;

			// fourth stage
			block[blk + 8 * 0] = CLIP[16384 + DESCALE(tmp7 + tmp1, 14)];
			block[blk + 8 * 1] = CLIP[16384 + DESCALE(tmp3 + tmp2, 14)];
			block[blk + 8 * 2] = CLIP[16384 + DESCALE(tmp0 + tmp4, 14)];
			block[blk + 8 * 3] = CLIP[16384 + DESCALE(tmp8 + tmp6, 14)];
			block[blk + 8 * 4] = CLIP[16384 + DESCALE(tmp8 - tmp6, 14)];
			block[blk + 8 * 5] = CLIP[16384 + DESCALE(tmp0 - tmp4, 14)];
			block[blk + 8 * 6] = CLIP[16384 + DESCALE(tmp3 - tmp2, 14)];
			block[blk + 8 * 7] = CLIP[16384 + DESCALE(tmp7 - tmp1, 14)];
		}
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}
}
