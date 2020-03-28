package examples;

import java.awt.image.BufferedImage;


public class _ImageQualityTest
{
	public double psnr;
	public long mse;
	public long pixelErrors;
	public long pixelDiff;
	public long accumDiff;


	public _ImageQualityTest(BufferedImage aImage1, BufferedImage aImage2, BufferedImage dst)
	{
		long deltaR = 0;
		long deltaG = 0;
		long deltaB = 0;
		int w = aImage1.getWidth();
		int h = aImage1.getHeight();

		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int c0 = aImage1.getRGB(x, y);
				int c1 = aImage2.getRGB(x, y);

				int r0 = 0xff & (c0 >> 16);
				int g0 = 0xff & (c0 >>  8);
				int b0 = 0xff & (c0      );
				int r1 = 0xff & (c1 >> 16);
				int g1 = 0xff & (c1 >>  8);
				int b1 = 0xff & (c1      );

				if (r0 != r1)
				{
					deltaR += Math.pow(r0 - r1, 2);
				}
				if (g0 != g1)
				{
					deltaG += Math.pow(g0 - g1, 2);
				}
				if (b0 != b1)
				{
					deltaB += Math.pow(b0 - b1, 2);
				}

				int dr = Math.abs(r0 - r1);
				int dg = Math.abs(g0 - g1);
				int db = Math.abs(b0 - b1);

				int d = dr + dg + db;
				accumDiff += d;

				if (d != 0)
				{
					pixelDiff++;
				}
				if (dr > 5 || dg > 5 || db > 5)
				{
					pixelErrors++;
				}

				if (dst != null)
				{
					dst.setRGB(x, y, (clamp(128 + r0 - r1) << 16) + (clamp(128 + g0 - g1) << 8) + clamp(128 + b0 - b1));
				}
			}
		}

		mse = (deltaR + deltaG + deltaB) / (w * h) / 3;

		psnr = mse == 0 ? 0 : -10 * Math.log10(mse / Math.pow(255, 2));
	}


	private static int clamp(int v)
	{
		return v < 0 ? 0 : v > 255 ? 255 : v;
	}


	@Override
	public String toString()
	{
		return String.format("{psnr=%6.3f, mse=%d, pErr=%d, pDif=%d, totDif=%d}", psnr, mse, pixelErrors, pixelDiff, accumDiff);
	}
}
