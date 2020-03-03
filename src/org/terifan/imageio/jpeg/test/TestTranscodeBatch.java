package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestTranscodeBatch
{
	public static void main(String... args)
	{
		try
		{
			int totalAriLength = 0;
			int totalAriProgLength = 0;
			int totalHuffProgLength = 0;
			int totalHuffOptLength = 0;
			int totalHuffLength = 0;

//			for (File dir : new File("D:\\Pictures").listFiles(e->e.isDirectory()))
			{
				for (File file : new File("D:\\Pictures\\Wallpapers High Quality").listFiles(e->e.getName().toLowerCase().endsWith(".jpg")))
				{
					byte[] data = new byte[(int)file.length()];
					try (FileInputStream in = new FileInputStream(file))
					{
						in.read(data);
					}

					try
					{
						ByteArrayOutputStream ariData = new ByteArrayOutputStream();
						ByteArrayOutputStream ariProgData = new ByteArrayOutputStream();
						ByteArrayOutputStream huffProgData = new ByteArrayOutputStream();
						ByteArrayOutputStream huffOptData = new ByteArrayOutputStream();
						ByteArrayOutputStream huffData = new ByteArrayOutputStream();

						new Transcode().setArithmetic(true).setProgressive(false).transcode(new ByteArrayInputStream(data), ariData);
						new Transcode().setArithmetic(true).setProgressive(true).transcode(new ByteArrayInputStream(data), ariProgData);
						new Transcode().setArithmetic(false).setProgressive(true).setOptimizedHuffman(true).transcode(new ByteArrayInputStream(data), huffProgData);
						new Transcode().setArithmetic(false).setProgressive(false).setOptimizedHuffman(true).transcode(new ByteArrayInputStream(data), huffOptData);
						new Transcode().setArithmetic(false).setProgressive(false).setOptimizedHuffman(false).transcode(new ByteArrayInputStream(data), huffData);

						File ariFile = new File("D:\\temp\\jpg-ari", file.getName());
						File ariProgFile = new File("D:\\temp\\jpg-ari-prog", file.getName());
						File huffFile = new File("D:\\temp\\jpg-huff", file.getName());
						File huffProgFile = new File("D:\\temp\\jpg-huff-prog", file.getName());
						File huffOptFile = new File("D:\\temp\\jpg-huff-opt", file.getName());

						try (FileOutputStream fos = new FileOutputStream(ariFile))
						{
							ariData.writeTo(fos);
						}

						try (FileOutputStream fos = new FileOutputStream(ariProgFile))
						{
							ariProgData.writeTo(fos);
						}

						try (FileOutputStream fos = new FileOutputStream(huffOptFile))
						{
							huffOptData.writeTo(fos);
						}

						try (FileOutputStream fos = new FileOutputStream(huffProgFile))
						{
							huffProgData.writeTo(fos);
						}

						try (FileOutputStream fos = new FileOutputStream(huffFile))
						{
							huffData.writeTo(fos);
						}

						BufferedImage imageAri = JPEGImageReader.read(ariFile);
						BufferedImage imageAriProg = JPEGImageReader.read(ariProgFile);
						BufferedImage imageHuff = JPEGImageReader.read(huffFile);
						BufferedImage imageHuffProg = JPEGImageReader.read(huffProgFile);
						BufferedImage imageHuffOpt = JPEGImageReader.read(huffOptFile);

						boolean err = compare(imageAri, imageAriProg) != 0 || compare(imageAri, imageHuff) != 0 || compare(imageAri, imageHuffOpt) != 0 || compare(imageAri, imageHuffProg) != 0;

						String z = compare(imageAri, imageAriProg)+" "+compare(imageHuff, imageHuffOpt)+" "+compare(imageHuff, imageHuffProg)+" "+compare(imageHuff, imageAri)+" "+compare(imageHuff, imageAriProg)+" "+compare(imageHuffProg, imageAri)+" "+compare(imageHuffProg, imageAriProg);

						System.out.printf("%-50s  %-5s  ari=%8d  ariProg=%8d  huff=%8d  huffProg=%8d  huffOpt=%8d  %s%n", z, err?"ERROR":"OK", ariData.size(), ariProgData.size(), huffData.size(), huffProgData.size(), huffOptData.size(), file.getName());

						totalAriLength += ariData.size();
						totalAriProgLength += ariProgData.size();
						totalHuffOptLength += huffOptData.size();
						totalHuffProgLength += huffProgData.size();
						totalHuffLength += huffData.size();
					}
					catch (Throwable e)
					{
						e.printStackTrace(System.out);
					}
				}
			}

			System.out.println();
			System.out.printf("ari=%8d ariProg=%8d huff=%8d huffProg=%8d huffOpt=%8d %n", totalAriLength/1024/1024, totalAriProgLength/1024/1024, totalHuffLength/1024/1024, totalHuffProgLength/1024/1024, totalHuffOptLength/1024/1024);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static int compare(BufferedImage aImage1, BufferedImage aImage2)
	{
		int err = 0;
		for (int y = 0; y < aImage2.getHeight(); y++)
		{
			for (int x = 0; x < aImage2.getWidth(); x++)
			{
				if (aImage2.getRGB(x, y) != aImage1.getRGB(x, y))
				{
					err++;
				}
			}
		}
		return err;
	}
}
