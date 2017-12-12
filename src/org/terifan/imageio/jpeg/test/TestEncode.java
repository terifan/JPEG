package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriter;


public class TestEncode 
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			BufferedImage src = JPEGImageReader.read(TestEncode.class.getResourceAsStream("Swallowtail.jpg"));
			
			new JPEGImageWriter().write(src, 95, baos);

//			Debug.hexDump(baos.toByteArray());
			
			BufferedImage image = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));

			System.out.println(image);
			
			ImageFrame imagePane = new ImageFrame(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
