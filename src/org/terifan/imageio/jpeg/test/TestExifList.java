package org.terifan.imageio.jpeg.test;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.terifan.imageio.jpeg.exif.Exif;
import org.terifan.imageio.jpeg.exif.ExifEntry;
import org.terifan.imageio.jpeg.exif.ExifTag;
import org.terifan.imageio.jpeg.exif.JPEGExif;


public class TestExifList
{
	public static void main(String... args)
	{
		try
		{
			Files.walk(Paths.get(System.getProperty("user.home"), "Pictures")).filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".jpg")).forEach(path ->
			{
				try
				{
					Exif exif = JPEGExif.extract(Files.newInputStream(path));

					if (exif != null)
					{
						System.out.println(exif);
						ExifEntry entry = exif.get(ExifTag.valueOf("RatingNumber"));
						if (entry != null)
						{
							System.out.println(path);
							System.out.println(entry);
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace(System.out);
				}
			});
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
