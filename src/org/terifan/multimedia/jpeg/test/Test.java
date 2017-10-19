package org.terifan.multimedia.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.imageio.ImageIO;
import org.terifan.multimedia.jpeg.JPEGImageReader;


public class Test
{
	public static void main(String... args)
	{
		try
		{
//			File file = new File("d:\\pictures\\aliens3.jpg");
			File file = new File(Test.class.getResource("beautiful-river-hd-1080p-wallpapers-download.jpg").getPath());

			try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file)))
			{
				BufferedImage myImage = JPEGImageReader.read(input);

				BufferedImage javaImage = ImageIO.read(file);

				BufferedImage diff = new BufferedImage(myImage.getWidth(), myImage.getHeight(), BufferedImage.TYPE_INT_RGB);
				for (int y = 0; y < diff.getHeight(); y++)
				{
					for (int x = 0; x < diff.getWidth(); x++)
					{
						int s = 1;
						int r = 128 + s * ((255 & (myImage.getRGB(x, y) >> 16)) - (255 & (javaImage.getRGB(x, y) >> 16)));
						int g = 128 + s * ((255 & (myImage.getRGB(x, y) >> 8)) - (255 & (javaImage.getRGB(x, y) >> 8)));
						int b = 128 + s * ((255 & (myImage.getRGB(x, y))) - (255 & (javaImage.getRGB(x, y))));
						diff.setRGB(x, y, (r << 16) + (g << 8) + b);
					}
				}

				ImageFrame imagePane = new ImageFrame(diff);

				System.out.println(PSNR.calculate(myImage, javaImage));
				MeasureErrorRate.measureError(myImage, javaImage, 0, 0, file);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
