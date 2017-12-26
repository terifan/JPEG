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


public class TestTranscode
{
	public static void main(String... args)
	{
		try
		{
			JPEGConstants.VERBOSE = true;

//			File file = new File("D:\\Pictures\\MLP\\6f4625df512a20dd62bad3f1c6c8accb.jpg");
//			File file = new File("D:\\Pictures\\MLP\\ba798e6190d118e4c00b73e6d4fa08d9.jpg");
//			File file = new File("D:\\Pictures\\Wallpapers\\9674_forest.jpg");
//			File file = new File("D:\\Pictures\\Wallpapers\\apple-wood-1920x1080-wallpaper-3113.jpg");
//			File file = new File("D:\\Pictures\\Wallpapers\\autumn-landscape-wallpaper-1920x1080-1008099.jpg");
//			File file = new File("D:\\Pictures\\Wallpapers\\girl-wolf-friendship.jpg");
//			File file = new File("D:\\Pictures\\Wallpapers\\gold-coast-australia.jpg");
			File file = new File("D:\\Pictures\\Wallpapers\\superb-forest-wallpaper-1920x1080-1009097.jpg");

			BufferedImage image0 = JPEGImageReader.read(file);

			ImageFrame imagePane = new ImageFrame(image0);

//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//			System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//			System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//			System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//			System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//
//			new Transcode().transcode(file, baos);
//
//			System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//			System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//			System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//			System.out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//
//			BufferedImage image1 = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));
//
//			baos.writeTo(new FileOutputStream("d:/test.jpg"));
//
//			System.out.println(image1);
//
//			ImageFrame imagePane = new ImageFrame(image1);
//
//			int err = 0;
//			for (int y = 0; y < image0.getHeight(); y++)
//			{
//				for (int x = 0; x < image0.getWidth(); x++)
//				{
//					if (image0.getRGB(x, y) != image1.getRGB(x, y))
//					{
//						err++;
//					}
//				}
//			}
//			System.out.println(err);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
