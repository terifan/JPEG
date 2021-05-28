package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class TestTranscode2
{
	public static void main(String... args)
	{
		try
		{
			Stream.of(new File("D:\\Pictures\\Wallpapers Fantasy").listFiles(e->e.getName().matches("(i?).*jpg"))).parallel().forEach(file->
			{
				try
				{
					ByteArrayOutputStream ariImage = new ByteArrayOutputStream();

					new JPEGImageIO().setCompressionType(CompressionType.ArithmeticProgressive).transcode(file, ariImage);

					ByteArrayOutputStream hufImage = new ByteArrayOutputStream();

					new JPEGImageIO().setCompressionType(CompressionType.Huffman).transcode(ariImage.toByteArray(), hufImage);

					BufferedImage image1 = ImageIO.read(new ByteArrayInputStream(hufImage.toByteArray()));
					BufferedImage image2 = ImageIO.read(file);

					double err = MeasureErrorRate.measureError(image1, image2);

					if (err == 0)
					{
						System.out.print("OK ");
					}
					else
					{
						System.out.print("\n[" + err + "] " + file + " ");
					}
				}
				catch (Throwable e)
				{
					System.out.println(file + " ");
				}
			});
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
