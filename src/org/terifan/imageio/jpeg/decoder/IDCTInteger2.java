package org.terifan.imageio.jpeg.decoder;


public class IDCTInteger2
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

	public void inverse(int[] aBlock)
	{
		for (int ctr = 0; ctr < 64; ctr+=8)
		{
			int tmp0;
			int tmp1 = aBlock[ctr + 4] << 11;
			int tmp2 = aBlock[ctr + 6];
			int tmp3 = aBlock[ctr + 2];
			int tmp4 = aBlock[ctr + 1];
			int tmp5 = aBlock[ctr + 7];
			int tmp6 = aBlock[ctr + 5];
			int tmp7 = aBlock[ctr + 3];
			int tmp8;

			if (tmp1 == 0 && tmp2 == 0 && tmp3 == 0 && tmp4 == 0 && tmp5 == 0 && tmp6 == 0 && tmp7 == 0)
			{
				aBlock[ctr + 0] = aBlock[ctr + 1] = aBlock[ctr + 2] = aBlock[ctr + 3] = aBlock[ctr + 4] = aBlock[ctr + 5] = aBlock[ctr + 6] = aBlock[ctr + 7] = aBlock[ctr + 0] << 3;
				continue;
			}

			tmp0 = (aBlock[ctr + 0] << 11) + 128;

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

			aBlock[ctr + 0] = ((tmp7 + tmp1) >> 8);
			aBlock[ctr + 1] = ((tmp3 + tmp2) >> 8);
			aBlock[ctr + 2] = ((tmp0 + tmp4) >> 8);
			aBlock[ctr + 3] = ((tmp8 + tmp6) >> 8);
			aBlock[ctr + 4] = ((tmp8 - tmp6) >> 8);
			aBlock[ctr + 5] = ((tmp0 - tmp4) >> 8);
			aBlock[ctr + 6] = ((tmp3 - tmp2) >> 8);
			aBlock[ctr + 7] = ((tmp7 - tmp1) >> 8);
		}

		for (int ctr = 0; ctr < 8; ctr++)
		{
			int tmp0;
			int tmp1 = aBlock[ctr + 8 * 4] << 8;
			int tmp2 = aBlock[ctr + 8 * 6];
			int tmp3 = aBlock[ctr + 8 * 2];
			int tmp4 = aBlock[ctr + 8 * 1];
			int tmp5 = aBlock[ctr + 8 * 7];
			int tmp6 = aBlock[ctr + 8 * 5];
			int tmp7 = aBlock[ctr + 8 * 3];
			int tmp8;

			if (tmp1 == 0 && tmp2 == 0 && tmp3 == 0 && tmp4 == 0 && tmp5 == 0 && tmp6 == 0 && tmp7 == 0)
			{
				aBlock[ctr + 8 * 0] = aBlock[ctr + 8 * 1] = aBlock[ctr + 8 * 2] = aBlock[ctr + 8 * 3] = aBlock[ctr + 8 * 4] = aBlock[ctr + 8 * 5] = aBlock[ctr + 8 * 6] = aBlock[ctr + 8 * 7] = CLIP[16384 + ((aBlock[ctr + 8 * 0] + 32) >> 6)];
				continue;
			}

			tmp0 = (aBlock[ctr + 8 * 0] << 8) + 8192;

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
			aBlock[ctr + 8 * 0] = CLIP[16384 + DESCALE(tmp7 + tmp1, 14)];
			aBlock[ctr + 8 * 1] = CLIP[16384 + DESCALE(tmp3 + tmp2, 14)];
			aBlock[ctr + 8 * 2] = CLIP[16384 + DESCALE(tmp0 + tmp4, 14)];
			aBlock[ctr + 8 * 3] = CLIP[16384 + DESCALE(tmp8 + tmp6, 14)];
			aBlock[ctr + 8 * 4] = CLIP[16384 + DESCALE(tmp8 - tmp6, 14)];
			aBlock[ctr + 8 * 5] = CLIP[16384 + DESCALE(tmp0 - tmp4, 14)];
			aBlock[ctr + 8 * 6] = CLIP[16384 + DESCALE(tmp3 - tmp2, 14)];
			aBlock[ctr + 8 * 7] = CLIP[16384 + DESCALE(tmp7 - tmp1, 14)];
		}
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}
}
