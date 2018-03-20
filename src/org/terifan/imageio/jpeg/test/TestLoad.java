package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.File;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestLoad
{
	public static void main(String... args)
	{
		try
		{
			JPEGConstants.VERBOSE = true;

			BufferedImage myImage = JPEGImageReader.read(TestLoad.class.getResource("Swallowtail-ari-prog.jpg"));

			ImageFrame imagePane = new ImageFrame(myImage);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
