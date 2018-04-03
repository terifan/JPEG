package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestTranscode2
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			JPEGConstants.VERBOSE = true;
			
			System.out.println("=================================================================================================================================================================================");
			
			new Transcode().setArithmetic(true).setProgressive(true).setOptimizedHuffman(true).transcode(TestTranscode2.class.getResource("Swallowtail-ari-prog.jpg"), baos);
			
			System.out.println(baos.size());
			
			System.out.println("=================================================================================================================================================================================");

			try (FileOutputStream fos = new FileOutputStream("d:\\test.jpg"))
			{
				fos.write(baos.toByteArray());
			}
			
//			BufferedImage image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
			BufferedImage image = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));

			new ImageFrame(image);

//			System.out.println(ariImage);

		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
