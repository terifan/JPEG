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
			File file = new File("d:/1107.jpg");
			byte[] imageData = new byte[(int)file.length()];
			try (FileInputStream in = new FileInputStream(file))
			{
				in.read(imageData);
			}

			for (;;)
			{
				System.out.println(terifanImageReader(imageData) + "\t" + javaImageIO(imageData));
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