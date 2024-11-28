package org.terifan.imageio.jpeg.exif;

import examples.res.R;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class JPEGExifNGTest
{
	@Test
	public void testAddingTags() throws IOException
	{
		byte[] data1 = R.class.getResourceAsStream("Swallowtail.jpg").readAllBytes();

		Exif exif1 = JPEGExif.extract(data1);

		exif1.add(new ExifEntry(ExifTag.valueOf("RatingNumber"), 3));
		exif1.add(new ExifEntry(ExifTag.valueOf("RatingPercent"), 75));
		exif1.add(new ExifEntry(ExifTag.valueOf("DateTime"), "2020:06:12 16:58:42"));

		byte[] data2 = JPEGExif.replace(data1, exif1.encode());

		BufferedImage image1 = ImageIO.read(new ByteArrayInputStream(data1));
		BufferedImage image2 = ImageIO.read(new ByteArrayInputStream(data2));

		for (int y = 0; y < image1.getHeight(); y++)
		{
			for (int x = 0; x < image1.getWidth(); x++)
			{
				assertEquals(image2.getRGB(x, y), image1.getRGB(x, y));
			}
		}

		Exif exif2 = JPEGExif.extract(data2);

		assertEquals(exif2.get(ExifTag.valueOf("RatingNumber")).getValue(), 3);
		assertEquals(exif2.get(ExifTag.valueOf("RatingPercent")).getValue(), 75);

		exif2.add(new ExifEntry(ExifTag.valueOf("RatingNumber"), 2));
		exif2.add(new ExifEntry(ExifTag.valueOf("RatingPercent"), 50));

		byte[] data3 = JPEGExif.replace(data2, exif2.encode());
		BufferedImage image3 = ImageIO.read(new ByteArrayInputStream(data3));

		for (int y = 0; y < image1.getHeight(); y++)
		{
			for (int x = 0; x < image1.getWidth(); x++)
			{
				assertEquals(image3.getRGB(x, y), image1.getRGB(x, y));
			}
		}

		Exif exif3 = JPEGExif.extract(data3);

		System.out.println(exif3.get(ExifTag.valueOf("RatingNumber")));
		System.out.println(exif3.get(ExifTag.valueOf("RatingPercent")));
		System.out.println(exif3.get(ExifTag.valueOf("DateTime")));

		assertEquals(exif3.get(ExifTag.valueOf("RatingNumber")).getValue(), 2);
		assertEquals(exif3.get(ExifTag.valueOf("RatingPercent")).getValue(), 50);
		assertEquals(exif3.get(ExifTag.valueOf("DateTime")).getValue(), "2020:06:12 16:58:42");
	}
}
