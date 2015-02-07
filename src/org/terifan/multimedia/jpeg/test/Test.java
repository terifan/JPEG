package org.terifan.multimedia.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.terifan.io.Streams;
import org.terifan.multimedia.jpeg.JPEGImageReader;
import org.terifan.ui.ImagePane;
import org.terifan.util.StopWatch;
import org.terifan.util.log.Log;


public class Test
{
	public static void main(String[] args)
	{
		try
		{
			displayTest("D:\\temp");
		}
		catch (Exception e)
		{
			e.printStackTrace(Log.out);
			System.exit(0);
		}
	}


	static void showImage(String path) throws Exception
	{
		StopWatch stopWatch = new StopWatch();
		BufferedImage image = new JPEGImageReader(new BufferedInputStream(new FileInputStream(path))).read();
		stopWatch.stop();

		JFrame frame = new JFrame(path + " [" + stopWatch + ", " + image.getWidth() + "x" + image.getHeight() + "]");
		frame.add(new ImagePane(image));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
	}


	static void displayTest(String aPath) throws Exception
	{
		ImagePane canvas = new ImagePane();

		JFrame frame = new JFrame();
		frame.add(canvas);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		for (File file : new File(aPath).listFiles((f)->f.getName().endsWith(".jpg")))
		{
			BufferedImage image1;
			try
			{
				StopWatch stopWatch = new StopWatch();
				image1 = new JPEGImageReader(new BufferedInputStream(new FileInputStream(file))).read();
				stopWatch.stop();

				canvas.setImage(image1);
				canvas.repaint();
				
				BufferedImage image2 = ImageIO.read(file);
				if (image2.getWidth() != image1.getWidth() || image2.getHeight() != image1.getHeight())
				{
					throw new IOException("Image size diff");
				}

				int error = 0;
				int crit = 0;
				int max = -1000;
				int min = 1000;
				for (int y = 0; y < image1.getHeight(); y++)
				{
					for (int x = 0; x < image1.getWidth(); x++)
					{
						int c0 = image1.getRGB(x, y);
						int c1 = image2.getRGB(x, y);
						int r0 = 255 & (c0 >> 16);
						int g0 = 255 & (c0 >> 8);
						int b0 = 255 & (c0);
						int r1 = 255 & (c1 >> 16);
						int g1 = 255 & (c1 >> 8);
						int b1 = 255 & (c1);
						int er = Math.abs(r0-r1);
						int eg = Math.abs(g0-g1);
						int eb = Math.abs(b0-b1);
						max = Math.max(max, Math.max(r0-r1, Math.max(g0-g1, b0-b1)));
						min = Math.min(min, Math.min(r0-r1, Math.min(g0-g1, b0-b1)));
						error += er + eg + eb;
						if (er > 10 || eg > 10 || eb > 10)
						{
							crit++;
						}
					}
				}
				
				int epp = error / (image1.getWidth() * image1.getHeight());

				Log.out.printf("%s %6d %6d %8d %12d %8d %8d %8d %8d  %s\n", stopWatch, image1.getWidth(), image1.getHeight(), file.length(), error, epp, crit, min, max, file);
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


	static void speedTest1(String aFile) throws Exception
	{
		byte[] data = Streams.fetch(aFile);

		while (true)
		{
			StopWatch stopWatch = new StopWatch();
			for (int i = 0; i < 100; i++)
			{
				new JPEGImageReader(new ByteArrayInputStream(data)).read();
			}
			Log.out.println(stopWatch);
		}
	}


	static void speedTest2(String aFile) throws Exception
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(Streams.fetch(aFile));

		while (true)
		{
			StopWatch stopWatch = new StopWatch();
			{
				for (int i = 0; i < 10; i++)
				{
					stream.reset();
					new JPEGImageReader(stream).read();
				}
			}
			stopWatch.split();
			{
				for (int i = 0; i < 10; i++)
				{
					stream.reset();
					ImageIO.read(stream);
				}
			}
			stopWatch.stop();

			Log.out.println(stopWatch);
		}
	}
}