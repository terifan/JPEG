package org.terifan.multimedia.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.terifan.multimedia.jpeg.JPEGImageReader;
import org.terifan.ui.ImagePane;
import org.terifan.util.StopWatch;
import org.terifan.util.log.Log;


public class MeasureErrorRate
{
	public static void main(String[] args)
	{
		try
		{
			ImagePane canvas = new ImagePane();

			JFrame frame = new JFrame();
			frame.add(canvas);
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			displayTest(canvas, "D:\\temp");
		}
		catch (Exception e)
		{
			e.printStackTrace(Log.out);
			System.exit(0);
		}
	}


	private static void displayTest(ImagePane aCanvas, String aPath) throws Exception
	{
		for (File file : new File(aPath).listFiles((f)->f.getName().endsWith(".jpg")))
		{
			BufferedImage image1;
			try
			{
				StopWatch stopWatch = new StopWatch();
				image1 = JPEGImageReader.read(new BufferedInputStream(new FileInputStream(file)));
				stopWatch.stop();

				aCanvas.setImage(image1);
				aCanvas.repaint();
				
				BufferedImage image2 = ImageIO.read(file);
				if (image2.getWidth() != image1.getWidth() || image2.getHeight() != image1.getHeight())
				{
					throw new IOException("Image size diff");
				}

				measureError(image1, image2, stopWatch, file);
			}
			catch (IOException e)
			{
				if (e.toString().contains("Progressive images not supported."))
				{
					Log.out.println("Progressive images not supported: " + file);
				}
				else
				{
					Log.out.println(e.toString()+": " + file);
				}
			}
		}
	}


	private static void measureError(BufferedImage aImage1, BufferedImage aImage2, StopWatch aStopWatch, File aFile)
	{
		int accumError = 0;
		int critError = 0;
		int minDiv = 1000;
		int maxDiv = -1000;

		for (int y = 0; y < aImage1.getHeight(); y++)
		{
			for (int x = 0; x < aImage1.getWidth(); x++)
			{
				int c0 = aImage1.getRGB(x, y);
				int c1 = aImage2.getRGB(x, y);
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

		int errorPP = accumError / (aImage1.getWidth() * aImage1.getHeight());

		Log.out.printf("%s %6d %6d %8d %12d %8d %8d %8d %8d  %s\n", aStopWatch, aImage1.getWidth(), aImage1.getHeight(), aFile.length(), accumError, errorPP, critError, minDiv, maxDiv, aFile);
	}
}