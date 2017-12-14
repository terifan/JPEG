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
	public static void xmain(String... args)
	{
		try
		{
			BufferedImage image0 = JPEGImageReader.read(TestTranscode.class.getResourceAsStream("Swallowtail.jpg"));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			new Transcode().transcode(TestTranscode.class.getResourceAsStream("Swallowtail.jpg"), baos);

			System.out.println("---------> "+baos.size());

			BufferedImage image1 = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));

			baos.writeTo(new FileOutputStream("d:/test.jpg"));

			System.out.println(image1);

			ImageFrame imagePane = new ImageFrame(image1);

			int err = 0;
			for (int y = 0; y < image0.getHeight(); y++)
			{
				for (int x = 0; x < image0.getHeight(); x++)
				{
					if (image0.getRGB(x, y) != image1.getRGB(x, y))
					{
						err++;
					}
				}
			}
			System.out.println(err);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void main(String... args)
	{
		try
		{
			for (File file : new File("D:\\Pictures\\Wallpapers High Quality").listFiles())
			{
				System.out.println(file);

				try (FileInputStream in = new FileInputStream(file))
				{
					JPEGImageReader.read(in);

//					ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//					new Transcode().transcode(in, baos);
//
//					System.out.println("XXXXXXXXXXXXXXX " + baos.size()+"\t"+file.length());
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
