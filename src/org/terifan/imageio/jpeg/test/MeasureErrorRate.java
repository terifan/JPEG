package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;


public class MeasureErrorRate
{
	static double measureError(BufferedImage aImage, BufferedImage aComparisonImage)
	{
		long accumError = 0;

		for (int y = 0; y < aImage.getHeight(); y++)
		{
			for (int x = 0; x < aImage.getWidth(); x++)
			{
				int c0 = aImage.getRGB(x, y);
				int c1 = aComparisonImage.getRGB(x, y);

				int r0 = 255 & (c0 >> 16);
				int g0 = 255 & (c0 >> 8);
				int b0 = 255 & (c0);

				int r1 = 255 & (c1 >> 16);
				int g1 = 255 & (c1 >> 8);
				int b1 = 255 & (c1);

				int er = Math.abs(r0 - r1);
				int eg = Math.abs(g0 - g1);
				int eb = Math.abs(b0 - b1);

				accumError += er + eg + eb;
			}
		}

		return accumError / (double)(aImage.getWidth() * aImage.getHeight());
	}
}
