package org.terifan.imageio.jpeg.examples;

import java.awt.image.BufferedImage;
import java.io.File;
import org.terifan.imageio.jpeg.encoder.JPEGImageIO;
import org.terifan.imageio.jpeg.examples.res.R;
import org.terifan.imageio.jpeg.test.ImageFrame;


public class SaveJPEGDemo
{
	public static void main(String ... args)
	{
		try
		{
			BufferedImage myImage = new JPEGImageIO().read(R.class.getResource("Swallowtail-ari.jpg"));

			File output = new File("d:\\Swallowtail-arithmetic.jpg");

			new JPEGImageIO().setArithmetic(true).setProgressive(true).setQuality(55).setProgressionScript(null).write(myImage, output);

			ImageFrame.show(output);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
