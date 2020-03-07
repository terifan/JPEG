package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class TestEncode
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			BufferedImage src = new JPEGImageIO().read(TestEncode.class.getResourceAsStream("Swallowtail.jpg"));

			System.out.println("¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");

			JPEGConstants.VERBOSE = true;

			new JPEGImageIO()
				.setArithmetic(!true)
				.setOptimizedHuffman(true)
				.setProgressive(true)
				.setQuality(55)
				.write(src, baos);

//			JPEGConstants.VERBOSE = false;

			System.out.println("¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤¤");

			System.out.println("---------> "+baos.size());

			try (FileOutputStream fos = new FileOutputStream("d:\\test.jpg"))
			{
				baos.writeTo(fos);
			}

//			Debug.hexDump(baos.toByteArray());

			BufferedImage image = new JPEGImageIO().read(new ByteArrayInputStream(baos.toByteArray()));
//			BufferedImage image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));

			System.out.println(image);

			ImageFrame.show(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
