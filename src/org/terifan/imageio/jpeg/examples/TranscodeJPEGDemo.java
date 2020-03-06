package org.terifan.imageio.jpeg.examples;

import java.io.FileOutputStream;
import java.net.URL;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.examples.res.R;


public class TranscodeJPEGDemo
{
	public static void main(String... args)
	{
		try
		{
			URL input = R.class.getResource("Swallowtail.jpg");

			try (FileOutputStream output = new FileOutputStream("d:\\Swallowtail-transcoded.jpg"))
			{
				new Transcode().setArithmetic(false).setOptimizedHuffman(true).setProgressive(true).transcode(input, output);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
