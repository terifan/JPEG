package org.terifan.imageio.jpeg.test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestHuff
{
	public static void main(String... args)
	{
		try
		{
//			File input = new File("D:\\Pictures\\Wallpapers\\RadioChamber_ROW9081025939_1920x1080.jpg");
//			File input = new File("D:\\Pictures\\Wallpapers\\apple-wood-1920x1080-wallpaper-3113.jpg");
//			File input = new File("D:\\Pictures\\Wallpapers\\love-wallpaper-1080p-HD-computer-background.jpg");
//			File input = new File("D:\\Pictures\\Wallpapers\\hd-wallpapers-autumn-romantic-wallpaper-nature-1920x1200-wallpaper.jpg");
			File input = new File("D:\\Pictures\\Wallpapers\\Park-Autumn.jpg");

			JPEGConstants.VERBOSE = true;

			BufferedImage myImage = JPEGImageReader.read(input);
			BufferedImage javaImage = ImageIO.read(input);

			BufferedImage diff = new BufferedImage(javaImage.getWidth(), javaImage.getHeight(), BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < javaImage.getHeight(); y++)
			{
				for (int x = 0; x < javaImage.getWidth(); x++)
				{
					int s = 20;

					int c0 = myImage.getRGB(x, y);
					int c1 = javaImage.getRGB(x, y);
					int r = s * Math.abs((255 & (c0 >> 16)) - (255 & (c1 >> 16)));
					int g = s * Math.abs((255 & (c0 >> 8)) - (255 & (c1 >> 8)));
					int b = s * Math.abs((255 & (c0 >> 0)) - (255 & (c1 >> 0)));
//					int r = 128 + s * ((255 & (c0 >> 16)) - (255 & (c1 >> 16)));
//					int g = 128 + s * ((255 & (c0 >> 8)) - (255 & (c1 >> 8)));
//					int b = 128 + s * ((255 & (c0 >> 0)) - (255 & (c1 >> 0)));
					diff.setRGB(x, y, (r << 16) + (g << 8) + b);

//					int[] yuv = new int[3];
//					ColorSpace.rgbToYuvFloat(c1, yuv);
//					diff.setRGB(x, y, (yuv[2] << 16) + (yuv[2] << 8) + yuv[2]);
				}
			}

			BufferedImage image = new BufferedImage(myImage.getWidth() * 2, myImage.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.drawImage(myImage, 0 * javaImage.getWidth(), 0 * javaImage.getHeight(), null);
			g.drawImage(diff, 0 * javaImage.getWidth(), 1 * javaImage.getHeight(), null);
			g.drawImage(javaImage, 1 * javaImage.getWidth(), 1 * javaImage.getHeight(), null);
			g.dispose();
			
			System.out.println("\nError per pixel: " + MeasureErrorRate.measureError(javaImage, myImage));

			ImageIO.write(myImage, "png", new File("d:\\temp\\" + input.getName() + "_my.png"));
			ImageIO.write(javaImage, "png", new File("d:\\temp\\" + input.getName() + "_java.png"));
			ImageIO.write(diff, "png", new File("d:\\temp\\" + input.getName() + "_delta.png"));
			
			ImageFrame imagePane = new ImageFrame(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
