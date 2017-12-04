package org.terifan.multimedia.jpeg.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.imageio.ImageIO;
import org.terifan.multimedia.jpeg.JPEGImageReader;


public class PerformanceTest
{
	public static void main(String[] args)
	{
		try
		{
			File file = new File(Test.class.getResource("beautiful-river-hd-1080p-wallpapers-download.jpg").getPath());

			byte[] imageData = new byte[(int)file.length()];
			try (FileInputStream in = new FileInputStream(file))
			{
				in.read(imageData);
			}

			long a = 0;
			long b = 0;
			for (int n = 0;;)
			{
				a += terifanImageReader(imageData);
				b += javaImageIO(imageData);
				n++;
				
				System.out.println(a/n + "\t" + b/n);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
			System.exit(0);
		}
	}


	static long terifanImageReader(byte[] aImageData) throws Exception
	{
		long t = System.currentTimeMillis();
		JPEGImageReader.read(new ByteArrayInputStream(aImageData));

		return System.currentTimeMillis() - t;
	}


	static long javaImageIO(byte[] aImageData) throws Exception
	{
		long t = System.currentTimeMillis();
		ImageIO.read(new ByteArrayInputStream(aImageData));

		return System.currentTimeMillis() - t;
	}
}