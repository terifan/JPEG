package org.terifan.imageio.jpeg.examples;

import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.examples.res.R;


public class LoadJPEGDemo
{
	public static void main(String ... args)
	{
		try
		{
//			BufferedImage myImage = new JPEGImageIO().read(R.class.getResource("Swallowtail-huff-def.jpg"));
//			BufferedImage myImage = new JPEGImageIO().read(R.class.getResource("Swallowtail-huff-opt.jpg"));
//			BufferedImage myImage = new JPEGImageIO().read(R.class.getResource("Swallowtail-huff-opt-prog.jpg"));
//			BufferedImage myImage = new JPEGImageIO().read(R.class.getResource("Swallowtail-ari.jpg"));
//			BufferedImage myImage = new JPEGImageIO().read(R.class.getResource("Swallowtail-ari-prog.jpg"));

			BufferedImage myImage = new JPEGImageIO().setLog(System.out).read("d:\\dev\\Untitled.jpg");

			_ImageWindow.show(myImage);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
