package org.terifan.imageio.jpeg.examples;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriter;
import org.terifan.imageio.jpeg.examples.res.R;
import org.terifan.imageio.jpeg.test.ImageFrame;


public class SaveJPEGDemo
{
	public static void main(String ... args)
	{
		try
		{
			BufferedImage myImage = JPEGImageReader.read(R.class.getResource("Swallowtail-ari.jpg"));

			try (FileOutputStream output = new FileOutputStream("d:\\Swallowtail-arithmetic.jpg"))
			{
				new JPEGImageWriter(output).setArithmetic(true).setProgressive(true).write(myImage, 90);
			}

			myImage = JPEGImageReader.read(new File("d:\\Swallowtail-arithmetic.jpg"));

			ImageFrame imagePane = new ImageFrame(myImage);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
