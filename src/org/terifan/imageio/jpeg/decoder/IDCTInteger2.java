package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.DQTMarkerSegment;


public class IDCTInteger2 implements IDCT
{
	private final static int W1 = 2841;
	private final static int W2 = 2676;
	private final static int W3 = 2408;
	private final static int W5 = 1609;
	private final static int W6 = 1108;
	private final static int W7 = 565;


	@Override
	public void transform(int[] aCoefficients, DQTMarkerSegment aQuantizationTable)
	{
		for (int i = 0; i < 64; i++) aCoefficients[i] *= aQuantizationTable.getTableInt()[i];

		transform(aCoefficients);
	}


	public void transform(int[] aCoefficients)
	{
		int[] workspace = new int[64];

		for (int ctr = 0; ctr < 64; ctr+=8)
		{
			int tmp0;
			int tmp1 = aCoefficients[ctr + 4] << 11;
			int tmp2 = aCoefficients[ctr + 6];
			int tmp3 = aCoefficients[ctr + 2];
			int tmp4 = aCoefficients[ctr + 1];
			int tmp5 = aCoefficients[ctr + 7];
			int tmp6 = aCoefficients[ctr + 5];
			int tmp7 = aCoefficients[ctr + 3];
			int tmp8;

//			if (tmp1 == 0 && tmp2 == 0 && tmp3 == 0 && tmp4 == 0 && tmp5 == 0 && tmp6 == 0 && tmp7 == 0)
//			{
//				aCoefficients[ctr + 0] = aCoefficients[ctr + 1] = aCoefficients[ctr + 2] = aCoefficients[ctr + 3] = aCoefficients[ctr + 4] = aCoefficients[ctr + 5] = aCoefficients[ctr + 6] = aCoefficients[ctr + 7] = aCoefficients[ctr + 0] << 3;
//				continue;
//			}

			tmp0 = (aCoefficients[ctr + 0] << 11) + 128;

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

			workspace[ctr + 0] = ((tmp7 + tmp1) >> 8);
			workspace[ctr + 1] = ((tmp3 + tmp2) >> 8);
			workspace[ctr + 2] = ((tmp0 + tmp4) >> 8);
			workspace[ctr + 3] = ((tmp8 + tmp6) >> 8);
			workspace[ctr + 4] = ((tmp8 - tmp6) >> 8);
			workspace[ctr + 5] = ((tmp0 - tmp4) >> 8);
			workspace[ctr + 6] = ((tmp3 - tmp2) >> 8);
			workspace[ctr + 7] = ((tmp7 - tmp1) >> 8);
		}

		for (int ctr = 0; ctr < 8; ctr++)
		{
			int tmp0;
			int tmp1 = workspace[ctr + 8 * 4] << 8;
			int tmp2 = workspace[ctr + 8 * 6];
			int tmp3 = workspace[ctr + 8 * 2];
			int tmp4 = workspace[ctr + 8 * 1];
			int tmp5 = workspace[ctr + 8 * 7];
			int tmp6 = workspace[ctr + 8 * 5];
			int tmp7 = workspace[ctr + 8 * 3];
			int tmp8;

//			if (tmp1 == 0 && tmp2 == 0 && tmp3 == 0 && tmp4 == 0 && tmp5 == 0 && tmp6 == 0 && tmp7 == 0)
//			{
//				aCoefficients[ctr + 8 * 0] = aCoefficients[ctr + 8 * 1] = aCoefficients[ctr + 8 * 2] = aCoefficients[ctr + 8 * 3] = aCoefficients[ctr + 8 * 4] = aCoefficients[ctr + 8 * 5] = aCoefficients[ctr + 8 * 6] = aCoefficients[ctr + 8 * 7] = clamp(((workspace[ctr + 8 * 0] + 32) >> 6));
//				continue;
//			}

			tmp0 = (workspace[ctr + 8 * 0] << 8) + 8192;

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
			aCoefficients[ctr + 8 * 0] = clamp(DESCALE(tmp7 + tmp1, 14));
			aCoefficients[ctr + 8 * 1] = clamp(DESCALE(tmp3 + tmp2, 14));
			aCoefficients[ctr + 8 * 2] = clamp(DESCALE(tmp0 + tmp4, 14));
			aCoefficients[ctr + 8 * 3] = clamp(DESCALE(tmp8 + tmp6, 14));
			aCoefficients[ctr + 8 * 4] = clamp(DESCALE(tmp8 - tmp6, 14));
			aCoefficients[ctr + 8 * 5] = clamp(DESCALE(tmp0 - tmp4, 14));
			aCoefficients[ctr + 8 * 6] = clamp(DESCALE(tmp3 - tmp2, 14));
			aCoefficients[ctr + 8 * 7] = clamp(DESCALE(tmp7 - tmp1, 14));
		}
	}


	private static int clamp(int aValue)
	{
		aValue = 128 + (aValue >> 5);

		return aValue < 0 ? 0 : aValue > 255 ? 255 : aValue;
	}


	private static int DESCALE(int x, int n)
	{
		return (x + (1 << (n - 1))) >> n;
	}
}
