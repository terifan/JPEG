package org.terifan.imageio.jpeg.examples;

import java.awt.image.BufferedImage;
import java.io.File;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.JPEGImageIO;
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

			JPEGConstants.VERBOSE = true;

			new JPEGImageIO().setArithmetic(!true).setOptimizedHuffman(true).setProgressive(true).setQuality(95).setProgressionScript(null).setLog(System.out).write(myImage, output);

			JPEGConstants.VERBOSE = false;

//			System.out.println("************************************************************************************************************************************");
//			System.out.println("************************************************************************************************************************************");
//			System.out.println("************************************************************************************************************************************");
//			System.out.println("************************************************************************************************************************************");

			ImageFrame.show(output).setTitle("" + output.length());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
