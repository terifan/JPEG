package org.terifan.multimedia.jpeg.test;

import org.terifan.multimedia.jpeg.*;
import java.awt.image.BufferedImage;
import static java.lang.Math.pow;
import static java.lang.Math.log10;


class PSNR
{
	public static double calculate(BufferedImage aImage1, BufferedImage aImage2)
	{
		double[] peak = new double[3];
		double[] noise = new double[3];

		for (int y = 0; y < aImage1.getHeight(); y++)
		{
			for (int x = 0; x < aImage1.getWidth(); x++)
			{
				for (int c = 0; c < 3; c++)
				{
					int i = 0xff & (aImage1.getRGB(x, y) >> (8 * c));
					int j = 0xff & (aImage2.getRGB(x, y) >> (8 * c));

					noise[c] += pow(i - j, 2);
					if (peak[c] < i)
					{
						peak[c] = i;
					}
				}
			}
		}

		double db = 0;

		for (int c = 0; c < 3; c++)
		{
			double mse = noise[c] / (aImage1.getWidth() * aImage1.getHeight());

//			System.out.println(c+":");
//			System.out.println("  MSE: " + mse);
//			System.out.println("  PSNR(max=255): " + (10 * log10(255 * 255 / mse)));
//			System.out.println("  PSNR(max=" + peak[c] + "): " + 10 * log10(pow(peak[c], 2) / mse));

			db += 10 * log10(pow(peak[c], 2) / mse);
		}

		return db / 3;
	}
}
