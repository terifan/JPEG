package org.terifan.multimedia.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.terifan.multimedia.jpeg.JPEGImageReader;


public class MeasureErrorRate
{
	public static void main(String[] args)
	{
		try
		{
			displayTest("D:\\Pictures\\Wallpapers");
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void displayTest(String aPath) throws Exception
	{
		for (File file : new File(aPath).listFiles(f->f.getName().endsWith(".jpg")))
		{
			try
			{
				byte[] buffer = new byte[(int)file.length()];
				try (FileInputStream fis = new FileInputStream(file))
				{
					fis.read(buffer);
				}

				long t0 = System.nanoTime();
				BufferedImage imageThis = JPEGImageReader.read(new ByteArrayInputStream(buffer));
				long t1 = System.nanoTime();
				BufferedImage imageJava = ImageIO.read(new ByteArrayInputStream(buffer));
				long t2 = System.nanoTime();

				measureError(imageThis, imageJava, t1-t0, t2-t1, file);

				if (imageThis.getWidth() != imageJava.getWidth() || imageThis.getHeight() != imageJava.getHeight())
				{
					throw new IOException("Image size diff");
				}
			}
			catch (IOException e)
			{
				if (e.toString().contains("Progressive images not supported."))
				{
					System.out.println("Progressive images not supported: " + file);
				}
				else
				{
					System.out.println(e.toString()+": " + file);
				}
			}
		}
	}


	private static void measureError(BufferedImage aImageThis, BufferedImage aImageJava, long aTimeThis, long aTimeJava, File aFile)
	{
		int accumError = 0;
		int critError = 0;
		int minDiv = 1000;
		int maxDiv = -1000;

		for (int y = 0; y < aImageThis.getHeight(); y++)
		{
			for (int x = 0; x < aImageThis.getWidth(); x++)
			{
				int c0 = aImageThis.getRGB(x, y);
				int c1 = aImageJava.getRGB(x, y);
				int r0 = 255 & (c0 >> 16);
				int g0 = 255 & (c0 >> 8);
				int b0 = 255 & (c0);
				int r1 = 255 & (c1 >> 16);
				int g1 = 255 & (c1 >> 8);
				int b1 = 255 & (c1);
				int er = Math.abs(r0 - r1);
				int eg = Math.abs(g0 - g1);
				int eb = Math.abs(b0 - b1);
				maxDiv = Math.max(maxDiv, Math.max(r0 - r1, Math.max(g0 - g1, b0 - b1)));
				minDiv = Math.min(minDiv, Math.min(r0 - r1, Math.min(g0 - g1, b0 - b1)));
				accumError += er + eg + eb;
				if (er > 10 || eg > 10 || eb > 10)
				{
					critError++;
				}
			}
		}

		int errorPP = accumError / (aImageThis.getWidth() * aImageThis.getHeight());

		System.out.printf("tT=%-6.1f tJ=%-6.1f sz=%9s L=%-7d ae=%-12d epp=%-8d ce=%-8d mind=%-8d maxd=%-8d  %s\n", aTimeThis/1000000.0, aTimeJava/1000000.0, aImageThis.getWidth()+"x"+aImageThis.getHeight(), aFile.length(), accumError, errorPP, critError, minDiv, maxDiv, aFile);
	}
}