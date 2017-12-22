package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestTranscode
{
	public static void main(String... args)
	{
		try
		{
			// progressive huffman 1x1 - D:\Pictures\Wallpapers High Quality\1920x1080-Wallpaper-Backgrounds.jpg
			// progressive huffman 1x1 - D:\Pictures\Wallpapers High Quality\1920x1080-Wallpaper-HD-05-Download.jpg
			// progressive huffman 2x2 - D:\Pictures\Wallpapers High Quality\7glyqHJ.jpg
			// progressive huffman 2x2 - D:\Pictures\Wallpapers High Quality\Fall-Desktop-Wallpaper-Widescreen-1080p.jpg
			// progressive huffman 2x2 - D:\Pictures\Wallpapers High Quality\wallpaper-1920-1080-Arkhipo-Osipovka-Russia-pics-Ni765423.jpg
			// progressive huffman 4x1 - D:\Pictures\Wallpapers High Quality\sea-cliff.jpg
			//

			File file = new File("D:\\Pictures\\Wallpapers High Quality\\7glyqHJ.jpg");

			BufferedImage image0 = JPEGImageReader.read(file);

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
//				for (int x = 0; x < image0.getHeight(); x++)
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
