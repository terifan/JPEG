package org.terifan.imageio.jpeg.examples;

import java.awt.image.BufferedImage;
import java.io.File;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.examples.res.R;
import org.terifan.imageio.jpeg.test.ImageFrame;


public class SaveJPEGDemo
{
	public static void main(String ... args)
	{
		try
		{
			BufferedImage myImage = new JPEGImageIO()
//				.setLog(System.out)
				.read(R.class.getResource("Swallowtail-huff-opt-prog.jpg"));

			System.out.println("************************************************************************************************************************************");
			System.out.println("************************************************************************************************************************************");
			System.out.println("************************************************************************************************************************************");
			System.out.println("************************************************************************************************************************************");

			File output = new File("d:\\Swallowtail-arithmetic.jpg");

			new JPEGImageIO().setArithmetic(!true).setOptimizedHuffman(true).setProgressive(true).setQuality(93.36).setProgressionScript(null)
//				.setLog(System.out)
				.write(myImage, output);

			myImage = new JPEGImageIO()
//				.setLog(System.out)
				.read(output);

			ImageFrame.show(myImage).setTitle("" + output.length());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
