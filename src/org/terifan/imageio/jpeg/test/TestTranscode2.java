package org.terifan.imageio.jpeg.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.Transcode;


public class TestTranscode2
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

//			JPEGConstants.VERBOSE = true;
			
			System.out.println("=================================================================================================================================================================================");
			
			new Transcode().setArithmetic(!true).setProgressive(true).setOptimizedHuffman(true).transcode(TestTranscode2.class.getResource("Swallowtail-ari.jpg"), baos);
			
			System.out.println(baos.size());
			
			System.out.println("=================================================================================================================================================================================");

			try (FileOutputStream fos = new FileOutputStream("d:\\test.jpg"))
			{
				fos.write(baos.toByteArray());
			}

			new ImageFrame(ImageIO.read(new ByteArrayInputStream(baos.toByteArray())));

//			BufferedImage ariImage = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));

//			System.out.println(ariImage);

		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
