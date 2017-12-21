package org.terifan.imageio.jpeg.test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestHuff
{
	public static void main(String... args)
	{
		try
		{
//			URL input = TestHuff.class.getResource("bad image.jpg");
			URL input = TestHuff.class.getResource("Swallowtail-huff-opt-prog.jpg");
//			URL input = TestHuff.class.getResource("Swallowtail-huff-def-prog.jpg");
//			URL input = TestHuff.class.getResource("Swallowtail-huff-def.jpg");
//			URL input = TestHuff.class.getResource("Swallowtail-huff-opt.jpg");
//			URL input = TestHuff.class.getResource("Swallowtail-ari.jpg");
//			URL input = TestHuff.class.getResource("Swallowtail-ari-prog.jpg");

			BufferedImage myImage0 = JPEGImageReader.read(input);

			BufferedImage myImage1 = null; //JPEGImageReader.read(TestHuff.class.getResource("Swallowtail-huff-opt.jpg"));

			BufferedImage javaImage = ImageIO.read(TestHuff.class.getResource("Swallowtail-huff-opt-prog.jpg"));

			BufferedImage diff = new BufferedImage(javaImage.getWidth(), javaImage.getHeight(), BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < javaImage.getHeight(); y++)
			{
				for (int x = 0; x < javaImage.getWidth(); x++)
				{
					int s = 10;

					int c0 = myImage0.getRGB(x, y);
					int c1 = javaImage.getRGB(x, y);
					int r = 128 + s*((255&(c0>>16)) - (255&(c1>>16)));
					int g = 128 + s*((255&(c0>>8)) - (255&(c1>>8)));
					int b = 128 + s*((255&(c0>>0)) - (255&(c1>>0)));
					diff.setRGB(x, y, (r<<16)+(g<<8)+b);
				}
			}

			BufferedImage image = new BufferedImage(myImage0.getWidth()*2, myImage0.getHeight()*2, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.drawImage(myImage0, 0*javaImage.getWidth(), 0*javaImage.getHeight(), null);
			if (myImage1!=null) g.drawImage(myImage1, 1*javaImage.getWidth(), 0*javaImage.getHeight(), null);
			g.drawImage(diff, 0*javaImage.getWidth(), 1*javaImage.getHeight(), null);
			g.drawImage(javaImage, 1*javaImage.getWidth(), 1*javaImage.getHeight(), null);
			g.dispose();

			ImageFrame imagePane = new ImageFrame(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
