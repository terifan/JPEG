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
			File file = new File("d:\\pictures\\aliens3.jpg");

			try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file)))
			{
				BufferedImage image = JPEGImageReader.read(input);

				ImageFrame imagePane = new ImageFrame(image);

				BufferedImage javaImage = ImageIO.read(file);
				
				System.out.println(PSNR.calculate(image, javaImage));
				MeasureErrorRate.measureError(image, javaImage, 0, 0, file);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}