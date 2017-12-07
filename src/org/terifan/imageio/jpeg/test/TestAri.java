package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestAri
{
	public static void main(String... args)
	{
		try
		{
			URL jpegResource = TestAri.class.getResource("Swallowtail-ari.jpg");

			try (InputStream input = jpegResource.openStream())
			{
				BufferedImage myImage = JPEGImageReader.read(input, IDCTFloat.class);

				ImageFrame imagePane = new ImageFrame(myImage);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
