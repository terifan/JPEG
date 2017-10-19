package org.terifan.multimedia.jpeg.test;

import java.awt.image.BufferedImage;
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
			test("D:\\Pictures\\Wallpapers High Quality");
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void test(String aPath) throws Exception
	{
		long sumTimeThis = 0;
		long sumTimeJava = 0;

		for (File file : new File(aPath).listFiles(f -> f.getName().endsWith(".jpg")))
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

				measureError(imageThis, imageJava, t1 - t0, t2 - t1, file);

				if (imageThis.getWidth() != imageJava.getWidth() || imageThis.getHeight() != imageJava.getHeight())
				{
					throw new IOException("Image size diff");
				}

				sumTimeThis += t1 - t0;
				sumTimeJava += t2 - t1;
			}
			catch (Exception e)
			{
				if (e.toString().contains("Progressive images not supported."))
				{
					System.out.println("Progressive images not supported: " + file);
				}
				else
				{
					System.out.println(e.toString() + ": " + file);
					e.printStackTrace(System.out);
				}
			}
		}

		System.out.println();
		System.out.printf("%.1f / %.1f\n", sumTimeThis / 1000000.0, sumTimeJava / 1000000.0);
	}


	static void measureError(BufferedImage aImage, BufferedImage aComparisonImage, long aTimeThis, long aTimeJava, File aFile)
	{
		int accumError = 0;
		int critError = 0;
		int maxErr = 0;

		for (int y = 0; y < aImage.getHeight(); y++)
		{
			for (int x = 0; x < aImage.getWidth(); x++)
			{
				int c0 = aImage.getRGB(x, y);
				int c1 = aComparisonImage.getRGB(x, y);
				int r0 = 255 & (c0 >> 16);
				int g0 = 255 & (c0 >> 8);
				int b0 = 255 & (c0);
				int r1 = 255 & (c1 >> 16);
				int g1 = 255 & (c1 >> 8);
				int b1 = 255 & (c1);
				int er = Math.abs(r0 - r1);
				int eg = Math.abs(g0 - g1);
				int eb = Math.abs(b0 - b1);
				maxErr = Math.max(Math.max(maxErr, er), Math.max(eg, eb));
				accumError += er + eg + eb;
				if (er > 10 || eg > 10 || eb > 10)
				{
					critError++;
				}
			}
		}

		double errorPP = accumError / (double)(aImage.getWidth() * aImage.getHeight());

		System.out.printf("tT=%-6.1f tJ=%-6.1f sz=%9s L=%-7d accumErr=%-12d errPerPixel=%-6.1f critErr=%-8d maxErr=%-8d  %s\n", aTimeThis / 1000000.0, aTimeJava / 1000000.0, aImage.getWidth() + "x" + aImage.getHeight(), aFile.length(), accumError, errorPP, critError, maxErr, aFile);
	}
}
