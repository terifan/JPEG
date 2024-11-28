package org.terifan.imageio.jpeg.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.terifan.imageio.jpeg.exif.Exif;
import org.terifan.imageio.jpeg.exif.ExifEntry;
import org.terifan.imageio.jpeg.exif.ExifTag;
import org.terifan.imageio.jpeg.exif.JPEGExif;


public class TestExifFavorite
{
	public static void main(String... args)
	{
		try
		{
			Path path = Paths.get(System.getProperty("user.home"), "Pictures/Lenna.jpg");

			byte[] data = Files.readAllBytes(path);

			Exif exif = JPEGExif.extract(data);

			exif.add(new ExifEntry(ExifTag.valueOf("RatingNumber"), 3));
			exif.add(new ExifEntry(ExifTag.valueOf("RatingPercent"), 75));

			data = JPEGExif.replace(data, exif.encode());

			Files.write(path, data);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
