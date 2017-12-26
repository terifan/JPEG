package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class X
{
	public static void main(String... args)
	{
		try
		{
//			JPEGConstants.VERBOSE = true;

//			JPEGImageReader._forcRGB=true;
			
//			cb=20;
//			cr=64;
//			File file1 = new File("D:\\Pictures\\Wallpapers\\superb-forest-wallpaper-1920x1080-1009097.jpg");
//			File file2 = new File("D:\\temp\\superb-forest-wallpaper-1920x1080-1009097.jpg");

//			cb=66;
//			cr=76;
//			File file1 = new File("D:\\Pictures\\Wallpapers\\autumn-landscape-wallpaper-1920x1080-1008099.jpg");
//			File file2 = new File("D:\\temp\\autumn-landscape-wallpaper-1920x1080-1008099.jpg");

//			cb=86;
//			cr=0;
			File file1 = new File("D:\\Pictures\\Wallpapers fantasy\\dragon-wallpaper-1920x1080-1009013.jpg");
			File file2 = new File("D:\\temp\\dragon-wallpaper-1920x1080-1009013.jpg");

			BufferedImage image1 = JPEGImageReader.read(file1);
			BufferedImage image2 = JPEGImageReader.read(file2);

			for (int y = 0; y < image1.getHeight(); y++)
			{
				for (int x = 0; x < image1.getWidth(); x++)
				{
					int c1 = image1.getRGB(x, y);
					int c2 = image2.getRGB(x, y);
					int r1 = 0xff & (c1 >> 16);
					int g1 = 0xff & (c1 >> 8);
					int b1 = 0xff & (c1 >> 0);
					int r2 = 0xff & (c2 >> 16);
					int g2 = 0xff & (c2 >> 8);
					int b2 = 0xff & (c2 >> 0);
					
					System.out.printf("%4d %4d %4d -- %4d %4d %4d -- %4d %4d %4d %n", r1, g1, b1, b2, g2, b2, r1-r2, g1-g2, b1-b2);
					
//					if (c1 != c2)
//					{
//					}
				}
			}

//			ImageFrame imagePane = new ImageFrame(image0);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
