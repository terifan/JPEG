package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.exif.JPEGExif;


public class TestTranscodeBatch
{
	public static void main(String... args)
	{
		try
		{
			for (File file : new File("D:\\Pictures\\Wallpapers High Quality").listFiles())
			{
				byte[] data = new byte[(int)file.length()];
				try (FileInputStream in = new FileInputStream(file))
				{
					in.read(data);
				}

				try
				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					BufferedImage image0 = JPEGImageReader.read(new ByteArrayInputStream(data));

					new Transcode().transcode(new ByteArrayInputStream(data), baos);

					BufferedImage image1 = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));

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

					System.out.printf("%8d %8d %8d %s%n", baos.size(), file.length(), err, file);

					if (err == 0)
					{
						try (FileOutputStream fos = new FileOutputStream(new File("D:\\temp\\jpg-ari\\" + file.getName())))
						{
							baos.writeTo(fos);
						}

						data = JPEGExif.replace(data, null);
						
						try (FileOutputStream fos = new FileOutputStream(new File("D:\\temp\\jpg-huff\\" + file.getName())))
						{
							fos.write(data);
						}
					}
				}
				catch (Throwable e)
				{
					System.out.println(file);
					e.printStackTrace(System.out);
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
