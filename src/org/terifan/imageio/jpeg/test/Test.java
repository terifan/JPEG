package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			URL jpegResource = Test.class.getResource("Swallowtail.jpg");

			BufferedImage orgImage = ImageIO.read(Test.class.getResource("Swallowtail.png"));
			BufferedImage javaImage = ImageIO.read(jpegResource);

			BufferedImage orgImagePaintNet = ImageIO.read(Test.class.getResource("Swallowtail_paintnet.png"));

			try (InputStream input = jpegResource.openStream())
			{
				BufferedImage myImage = JPEGImageReader.read(input, IDCTFloat.class);

//				BufferedImage diff = new BufferedImage(myImage.getWidth(), myImage.getHeight(), BufferedImage.TYPE_INT_RGB);
//				for (int y = 0; y < diff.getHeight(); y++)
//				{
//					for (int x = 0; x < diff.getWidth(); x++)
//					{
//						int s = 1;
//						int r = 128 + s * ((255 & (myImage.getRGB(x, y) >> 16)) - (255 & (javaImage.getRGB(x, y) >> 16)));
//						int g = 128 + s * ((255 & (myImage.getRGB(x, y) >> 8)) - (255 & (javaImage.getRGB(x, y) >> 8)));
//						int b = 128 + s * ((255 & (myImage.getRGB(x, y))) - (255 & (javaImage.getRGB(x, y))));
//						diff.setRGB(x, y, (r << 16) + (g << 8) + b);
//					}
//				}
//
//				ImageFrame imagePane = new ImageFrame(diff);

				ImageFrame imagePane = new ImageFrame(myImage);

				System.out.println(PSNR.calculate(myImage, orgImage));
				MeasureErrorRate.measureError(myImage, orgImage, 0, 0, null);

				System.out.println("------------------------");

				System.out.println(PSNR.calculate(javaImage, orgImage));
				MeasureErrorRate.measureError(javaImage, orgImage, 0, 0, null);

				System.out.println("------------------------");

				System.out.println(PSNR.calculate(orgImagePaintNet, orgImage));
				MeasureErrorRate.measureError(orgImagePaintNet, orgImage, 0, 0, null);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
