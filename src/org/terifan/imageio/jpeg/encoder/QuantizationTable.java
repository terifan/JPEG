package org.terifan.imageio.jpeg.encoder;

import org.terifan.imageio.jpeg.DQTMarkerSegment;


public class QuantizationTable
{
	public static DQTMarkerSegment buildQuantTable(int aQuality, int aComponent)
	{
		int W = 8;
		int H = 8;
		
		aQuality = Math.max(Math.min(aQuality, 100), 1);

		if (aQuality < 50)
		{
			aQuality = 5000 / aQuality;
		}
		else
		{
			aQuality = 200 - aQuality * 2;
		}

		int[] quantval = new int[W * H];

		if (aComponent > 2)
		{
			for (int i = 0; i < quantval.length; i++)
			{
				quantval[i] = 128 - (int)Math.round(127 * Math.cos(Math.PI / 2 * i / (double)(quantval.length - 1)));
			}

			for (int i = 0; i < quantval.length; i++)
			{
				int temp = (quantval[i] * aQuality + 50) / 100;
				if (temp <= 0)
				{
					temp = 1;
				}
				if (temp > 255) // 32767 for 12-bit samles
				{
					temp = 255;
				}
				quantval[i] = temp;
			}
		}
		else
		{
			int[] std_luminance_quant_tbl =
			{
				16, 11, 10, 16, 24, 40, 51, 61,
				12, 12, 14, 19, 26, 58, 60, 55,
				14, 13, 16, 24, 40, 57, 69, 56,
				14, 17, 22, 29, 51, 87, 80, 62,
				18, 22, 37, 56, 68, 109, 103, 77,
				24, 35, 55, 64, 81, 104, 113, 92,
				49, 64, 78, 87, 103, 121, 120, 101,
				72, 92, 95, 98, 112, 100, 103, 99
			};
			int[] std_chrominance_quant_tbl =
			{
				17, 18, 24, 47, 99, 99, 99, 99,
				18, 21, 26, 66, 99, 99, 99, 99,
				24, 26, 56, 99, 99, 99, 99, 99,
				47, 66, 99, 99, 99, 99, 99, 99,
				99, 99, 99, 99, 99, 99, 99, 99,
				99, 99, 99, 99, 99, 99, 99, 99,
				99, 99, 99, 99, 99, 99, 99, 99,
				99, 99, 99, 99, 99, 99, 99, 99
			};

			int[] basic_table = aComponent == 0 ? std_luminance_quant_tbl : std_chrominance_quant_tbl;

			int sw = W / 8;
			int sh = H / 8;
			for (int y = 0, i = 0; y < H; y++)
			{
				for (int x = 0; x < W; x++, i++)
				{
					int temp = (basic_table[x / sw + y / sh * 8] * aQuality + 50) / 100;
					if (temp <= 0)
					{
						temp = 1;
					}
					if (temp > 255) // 32767 for 12-bit samles
					{
						temp = 255;
					}
					quantval[i] = temp;
				}
			}
		}

		return new DQTMarkerSegment(aComponent, quantval);
	}
}
