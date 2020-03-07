package org.terifan.imageio.jpeg.encoder;

import org.terifan.imageio.jpeg.QuantizationTable;


public class QuantizationTableFactory
{
	private final static int[] STD_LUMINANCE_QUANT_TBL =
	{
		16, 11, 10, 16,  24,  40,  51,  61,
		12, 12, 14, 19,  26,  58,  60,  55,
		14, 13, 16, 24,  40,  57,  69,  56,
		14, 17, 22, 29,  51,  87,  80,  62,
		18, 22, 37, 56,  68, 109, 103,  77,
		24, 35, 55, 64,  81, 104, 113,  92,
		49, 64, 78, 87, 103, 121, 120, 101,
		72, 92, 95, 98, 112, 100, 103,  99
	};

	private final static int[] STD_CHROMINANCE_QUANT_TBL =
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


	public static QuantizationTable buildQuantTable(int aQuality, int aComponent)
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

		if (aComponent > 1) // was > 2 ????
		{
			for (int i = 0; i < quantval.length; i++)
			{
				quantval[i] = 128 - (int)Math.round(127 * Math.cos(Math.PI / 2 * i / (double)(quantval.length - 1)));
			}

			for (int i = 0; i < quantval.length; i++)
			{
				quantval[i] = Math.max(1, Math.min(255, (quantval[i] * aQuality + 50) / 100));
			}
		}
		else
		{
			int[] table = aComponent == 0 ? STD_LUMINANCE_QUANT_TBL : STD_CHROMINANCE_QUANT_TBL;

			int sw = W / 8;
			int sh = H / 8;
			for (int y = 0, i = 0; y < H; y++)
			{
				for (int x = 0; x < W; x++, i++)
				{
					quantval[i] = Math.max(1, Math.min(255, (table[x / sw + y / sh * 8] * aQuality + 50) / 100));
				}
			}
		}

		return new QuantizationTable(aComponent, QuantizationTable.PRECISION_8_BITS, quantval);
	}
}
