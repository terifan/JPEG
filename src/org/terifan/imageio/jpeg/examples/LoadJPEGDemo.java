package org.terifan.imageio.jpeg.examples;

import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.encoder.JPEGImageIO;
import org.terifan.imageio.jpeg.examples.res.R;
import org.terifan.imageio.jpeg.test.ImageFrame;


public class LoadJPEGDemo
{
	public static void main(String ... args)
	{
		try
		{
			BufferedImage myImage = new JPEGImageIO().read(R.class.getResource("Swallowtail-ari.jpg"));

			ImageFrame.show(myImage);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
