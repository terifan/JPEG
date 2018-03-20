package org.terifan.imageio.jpeg.test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;
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

//			JPEGConstants.VERBOSE = true;
			
			System.out.println("=================================================================================================================================================================================");
			
			new Transcode().setArithmetic(true).setProgressive(true).transcode(TestTranscode2.class.getResource("Swallowtail-ari.jpg"), baos);
			
			System.out.println(baos.size());
			
			System.out.println("=================================================================================================================================================================================");

			BufferedImage ariImage = JPEGImageReader.read(new ByteArrayInputStream(baos.toByteArray()));

//			System.out.println(ariImage);

		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
