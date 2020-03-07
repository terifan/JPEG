package org.terifan.imageio.jpeg.examples;

import java.io.File;
import java.net.URL;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.examples.res.R;
import org.terifan.imageio.jpeg.test.ImageFrame;


public class TranscodeJPEGDemo
{
	public static void main(String... args)
	{
		try
		{
			URL input = R.class.getResource("Swallowtail.jpg");

			File output = new File("d:\\Swallowtail-arithmetic.jpg");

			new JPEGImageIO().setArithmetic(true).setProgressive(true).transcode(input, output);

			ImageFrame.show(output);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
