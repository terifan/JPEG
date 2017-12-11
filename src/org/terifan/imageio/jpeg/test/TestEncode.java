package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriter;


public class TestEncode 
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			new JPEGImageWriter().write(ImageIO.read(TestEncode.class.getResource("sample.jpg")), baos);

			Debug.hexDump(baos.toByteArray());
			
			BufferedImage image = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));

			System.out.println(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
