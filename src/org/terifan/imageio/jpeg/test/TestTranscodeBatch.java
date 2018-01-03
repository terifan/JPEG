package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.exif.JPEGExif;


public class TestTranscodeBatch
{
	public static void main(String... args)
	{
		try
		{
			int totalOriginalLength = 0;
			int totalCleanLength = 0;
			int totalAriLength = 0;
			
//			for (File dir : new File("D:\\Pictures").listFiles(e->e.isDirectory()))
			{
//				for (File file : dir.listFiles(e->e.getName().toLowerCase().endsWith(".jpg") && e.length() < 10000000))
				for (File file : new File("D:\\Pictures\\y").listFiles())
//				File file = new File("D:\\Pictures\\Wallpapers\\1 (1).jpg");
				{
					byte[] data = new byte[(int)file.length()];
					try (FileInputStream in = new FileInputStream(file))
					{
						in.read(data);
					}

					try
					{
						ByteArrayOutputStream ariData = new ByteArrayOutputStream();

						JPEGImageReader reader = new JPEGImageReader(new ByteArrayInputStream(data));
						BufferedImage imageHuff = reader.read();

						new Transcode().transcode(new ByteArrayInputStream(data), ariData);

						BufferedImage imageAri = JPEGImageReader.read(new ByteArrayInputStream(ariData.toByteArray()));

						ImageIO.write(imageAri, "png", new File("D:\\temp\\jpg-test\\" + file.getName().replace("jpg", "png")));

						BufferedImage javaImage = ImageIO.read(new ByteArrayInputStream(data));

						double eppJH = MeasureErrorRate.measureError(javaImage, imageHuff);
						double eppAH = MeasureErrorRate.measureError(imageAri, imageHuff);

						totalOriginalLength += data.length;

						data = JPEGExif.replace(data, null);

						System.out.printf("%s %6.3f %8d %8d %6.3f %s%n", reader.getSubSampling(), eppJH, ariData.size(), data.length, eppAH, file);

						try (FileOutputStream fos = new FileOutputStream(new File("D:\\temp\\jpg-ari\\" + file.getName())))
						{
							ariData.writeTo(fos);
						}

						try (FileOutputStream fos = new FileOutputStream(new File("D:\\temp\\jpg-huff\\" + file.getName())))
						{
							fos.write(data);
						}
						
						totalCleanLength += data.length;
						totalAriLength += ariData.size();
					}
					catch (Throwable e)
					{
						e.printStackTrace(System.out);
					}
				}
			}

			System.out.println();
			System.out.printf("original=%.2f, clean=%.2f, arithmetic=%.2f (%.2f)%n", totalOriginalLength/1024.0/1024, totalCleanLength/1024.0/1024, totalAriLength/1024.0/1024, 100-totalAriLength*100.0/totalCleanLength);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
