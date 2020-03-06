package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.encoder.JPEGImageIO;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestTranscode2
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

//			JPEGConstants.VERBOSE = true;

			System.out.println("=================================================================================================================================================================================");

			BufferedImage imagex = new JPEGImageIO().read(TestTranscode2.class.getResource("Swallowtail-ari-prog.jpg"));

			new JPEGImageIO().setArithmetic(true).setProgressive(true).setOptimizedHuffman(true).transcode(TestTranscode2.class.getResource("Swallowtail-ari-prog.jpg"), baos);
//			new Transcode().setArithmetic(true).setProgressive(true).setOptimizedHuffman(true).transcode(new FileInputStream("d:\\ari-test.jpg"), baos);

			System.out.println(baos.size());

			System.out.println("=================================================================================================================================================================================");

			try (FileOutputStream fos = new FileOutputStream("d:\\test.jpg"))
			{
				fos.write(baos.toByteArray());
			}

//			BufferedImage image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
			BufferedImage image = new JPEGImageIO().read(new ByteArrayInputStream(baos.toByteArray()));

			ImageFrame.show(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
