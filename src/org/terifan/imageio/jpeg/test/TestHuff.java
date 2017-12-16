package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestHuff
{
	public static void main(String... args)
	{
		try
		{
			URL jpegResource = TestAri.class.getResource("Swallowtail-huff-opt-prog.jpg");
//			URL jpegResource = TestAri.class.getResource("Swallowtail-huff-def-prog.jpg");
//			URL jpegResource = TestAri.class.getResource("Swallowtail-huff-def.jpg");
//			URL jpegResource = TestAri.class.getResource("Swallowtail-huff-opt.jpg");
//			URL jpegResource = TestAri.class.getResource("Swallowtail-ari.jpg");
//			URL jpegResource = TestAri.class.getResource("Swallowtail-ari-prog.jpg");

			try (InputStream input = jpegResource.openStream())
			{
				BufferedImage myImage = JPEGImageReader.read(input);

				ImageFrame imagePane = new ImageFrame(myImage);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
