package org.terifan.imageio.jpeg.test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestTranscode
{
	public static void main(String... args)
	{
		try
		{
			File file = new File("D:\\Pictures\\Wallpapers\\mountain-silhouette.jpg");

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			JPEGConstants.VERBOSE = true;
			
			BufferedImage huffImage = JPEGImageReader.read(file);

			System.out.println("=================================================================================================================================================================================");
			
			new Transcode().transcode(file, baos);

			System.out.println("=================================================================================================================================================================================");

			BufferedImage ariImage = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));

			JPEGConstants.VERBOSE = false;

			BufferedImage javaImage = ImageIO.read(file);

			BufferedImage diff = new BufferedImage(javaImage.getWidth(), javaImage.getHeight(), BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < javaImage.getHeight(); y++)
			{
				for (int x = 0; x < javaImage.getWidth(); x++)
				{
					int s = 20;

					int c0 = ariImage.getRGB(x, y);
					int c1 = huffImage.getRGB(x, y);
					int r = s * Math.abs((255 & (c0 >> 16)) - (255 & (c1 >> 16)));
					int g = s * Math.abs((255 & (c0 >> 8)) - (255 & (c1 >> 8)));
					int b = s * Math.abs((255 & (c0 >> 0)) - (255 & (c1 >> 0)));
					diff.setRGB(x, y, (r << 16) + (g << 8) + b);
				}
			}

			BufferedImage image = new BufferedImage(ariImage.getWidth() * 2, ariImage.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.drawImage(ariImage, 0 * javaImage.getWidth(), 0 * javaImage.getHeight(), null);
			g.drawImage(diff, 0 * javaImage.getWidth(), 1 * javaImage.getHeight(), null);
			g.drawImage(javaImage, 1 * javaImage.getWidth(), 1 * javaImage.getHeight(), null);
			g.dispose();
			
			System.out.println("\nError per pixel: " + MeasureErrorRate.measureError(huffImage, ariImage));

//			ImageIO.write(ariImage, "png", new File("d:\\temp\\" + file.getName() + "_my.png"));
//			ImageIO.write(javaImage, "png", new File("d:\\temp\\" + file.getName() + "_java.png"));
//			ImageIO.write(diff, "png", new File("d:\\temp\\" + file.getName() + "_delta.png"));
			
			ImageFrame imagePane = new ImageFrame(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
