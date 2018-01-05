package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriter;


public class TestEncode
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			BufferedImage src = JPEGImageReader.read(TestEncode.class.getResourceAsStream("Swallowtail.jpg"));

			System.out.println("¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");
			
			JPEGConstants.VERBOSE = true;

			new JPEGImageWriter(baos)
				.setArithmetic(!true)
				.setOptimizedHuffman(true)
				.setProgressive(true)
				.write(src, 55);

//			JPEGConstants.VERBOSE = false;

			System.out.println("¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");

			System.out.println("---------> "+baos.size());

			try (FileOutputStream fos = new FileOutputStream("d:\\test.jpg"))
			{
				baos.writeTo(fos);
			}

//			Debug.hexDump(baos.toByteArray());
			
			BufferedImage image = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));
//			BufferedImage image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));

			System.out.println(image);

			ImageFrame imagePane = new ImageFrame(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
