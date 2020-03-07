package org.terifan.imageio.jpeg.test;

import org.terifan.imageio.jpeg.examples._ImageWindow;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.examples.res.R;


public class TestTranscode
{
	public static void main(String... args)
	{
		try
		{
			URL file = R.class.getResource("Swallowtail.jpg");
//			URL file = new URL("file:///d:/dev/macos-catalina-cb-3840x2160-original.jpg");

			ByteArrayOutputStream transImageData = new ByteArrayOutputStream();


			BufferedImage originalImage = new JPEGImageIO().read(file);

			System.out.println("=================================================================================================================================================================================");

			new JPEGImageIO().setArithmetic(false).setOptimizedHuffman(true).setProgressive(true).transcode(file, transImageData);

//			transImageData.writeTo(new FileOutputStream("d:\\dev\\macos-catalina-cb-3840x2160-transcoded.jpg"));

			System.out.println(transImageData.size());

			System.out.println("=================================================================================================================================================================================");

//			JPEGConstants.VERBOSE = true;
//			BufferedImage transImage = JPEGImageReader.read(new URL("file:///d:/dev/macos-catalina-cb-3840x2160-transcoded.jpg"));
//			JPEGConstants.VERBOSE = false;

			BufferedImage transImage = new JPEGImageIO().read(new ByteArrayInputStream(transImageData.toByteArray()));
//			BufferedImage transImage = ImageIO.read(new ByteArrayInputStream(transImageData.toByteArray()));

			BufferedImage javaImage = ImageIO.read(file);

			BufferedImage deltaImage = new BufferedImage(javaImage.getWidth(), javaImage.getHeight(), BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < javaImage.getHeight(); y++)
			{
				for (int x = 0; x < javaImage.getWidth(); x++)
				{
					int s = 20;

					int c0 = transImage.getRGB(x, y);
					int c1 = originalImage.getRGB(x, y);
					int r = s * Math.abs((255 & (c0 >> 16)) - (255 & (c1 >> 16)));
					int g = s * Math.abs((255 & (c0 >> 8)) - (255 & (c1 >> 8)));
					int b = s * Math.abs((255 & (c0 >> 0)) - (255 & (c1 >> 0)));
					deltaImage.setRGB(x, y, (r << 16) + (g << 8) + b);
				}
			}

			BufferedImage image = new BufferedImage(transImage.getWidth() * 2, transImage.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.drawImage(originalImage, 0 * javaImage.getWidth(), 0 * javaImage.getHeight(), null);
			g.drawImage(transImage, 1 * javaImage.getWidth(), 0 * javaImage.getHeight(), null);
			g.drawImage(deltaImage, 0 * javaImage.getWidth(), 1 * javaImage.getHeight(), null);
			g.drawImage(javaImage, 1 * javaImage.getWidth(), 1 * javaImage.getHeight(), null);
			g.dispose();

			System.out.println("\nError per pixel: " + MeasureErrorRate.measureError(originalImage, transImage));

//			ImageIO.write(ariImage, "png", new File("d:\\temp\\" + file.getName() + "_my.png"));
//			ImageIO.write(javaImage, "png", new File("d:\\temp\\" + file.getName() + "_java.png"));
//			ImageIO.write(diff, "png", new File("d:\\temp\\" + file.getName() + "_delta.png"));

			_ImageWindow.show(image);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
