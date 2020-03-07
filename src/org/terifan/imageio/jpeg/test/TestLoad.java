package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class TestLoad
{
	public static void main(String... args)
	{
		try
		{
			JPEGConstants.VERBOSE = true;

			BufferedImage myImage = new JPEGImageIO().read(TestLoad.class.getResource("Swallowtail-ari-prog.jpg"));

			ImageFrame.show(myImage);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
