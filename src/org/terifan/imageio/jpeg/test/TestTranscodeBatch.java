package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.terifan.imageio.jpeg.encoder.JPEGImageIO;
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

			String out = "D:\\dev";

			for (File dir : new File("D:\\Pictures").listFiles(e->e.isDirectory()))
			{
				for (File file : dir.listFiles(e->e.getName().toLowerCase().endsWith(".jpg")))
//				for (File file : new File("D:\\Pictures\\Wallpapers High Quality").listFiles(e->e.getName().toLowerCase().endsWith(".jpg")))
//				for (File file : new File("C:\\Pictures\\Wallpapers High Quality").listFiles(e->e.getName().toLowerCase().endsWith(".jpg")))
				{
					byte[] data = new byte[(int)file.length()];
					try (FileInputStream in = new FileInputStream(file))
					{
						in.read(data);
					}

					try
					{
						File ariSeqFile = new File(new File(out, "jpg-ari"), file.getName());
						File ariProFile = new File(new File(out, "jpg-ari-prog"), file.getName());
						File hufSeqFile = new File(new File(out, "jpg-huff"), file.getName());
						File hufProFile = new File(new File(out, "jpg-huff-prog"), file.getName());
						File hufOptFile = new File(new File(out, "jpg-huff-opt"), file.getName());

						ByteArrayOutputStream ariSeqData = new ByteArrayOutputStream();
						ByteArrayOutputStream ariProData = new ByteArrayOutputStream();
						ByteArrayOutputStream hufProData = new ByteArrayOutputStream();
						ByteArrayOutputStream hufOptData = new ByteArrayOutputStream();
						ByteArrayOutputStream hufSeqData = new ByteArrayOutputStream();

						AtomicReference<BufferedImage> ariSeqImage = new AtomicReference<>();
						AtomicReference<BufferedImage> ariProImage = new AtomicReference<>();
						AtomicReference<BufferedImage> hufSeqImage = new AtomicReference<>();
						AtomicReference<BufferedImage> hufProImage = new AtomicReference<>();
						AtomicReference<BufferedImage> hufOptImage = new AtomicReference<>();

						AtomicInteger c0 = new AtomicInteger();
						AtomicInteger c1 = new AtomicInteger();
						AtomicInteger c2 = new AtomicInteger();
						AtomicInteger c3 = new AtomicInteger();
						AtomicInteger c4 = new AtomicInteger();
						AtomicInteger c5 = new AtomicInteger();
						AtomicInteger c6 = new AtomicInteger();
						AtomicInteger c7 = new AtomicInteger();
						AtomicInteger c8 = new AtomicInteger();
						AtomicInteger c9 = new AtomicInteger();

						try (FixedThreadExecutor ex  = new FixedThreadExecutor(1f))
						{
							ex.submit(()->process(data, ariSeqData, ariSeqFile, ariSeqImage, true, false, false));
							ex.submit(()->process(data, ariProData, ariProFile, ariProImage, true, true, false));
							ex.submit(()->process(data, hufSeqData, hufSeqFile, hufSeqImage, false, false, false));
							ex.submit(()->process(data, hufOptData, hufOptFile, hufOptImage, false, false, true));
							ex.submit(()->process(data, hufProData, hufProFile, hufProImage, false, true, false));
						}

						try (FixedThreadExecutor ex  = new FixedThreadExecutor(1f))
						{
							ex.submit(()->compare(c0, ariSeqImage, ariProImage));
							ex.submit(()->compare(c1, ariSeqImage, hufSeqImage));
							ex.submit(()->compare(c2, ariSeqImage, hufOptImage));
							ex.submit(()->compare(c3, ariSeqImage, hufProImage));
							ex.submit(()->compare(c4, ariProImage, hufOptImage));
							ex.submit(()->compare(c5, ariProImage, hufProImage));
							ex.submit(()->compare(c6, ariProImage, hufSeqImage));
							ex.submit(()->compare(c7, hufSeqImage, hufOptImage));
							ex.submit(()->compare(c8, hufSeqImage, hufProImage));
							ex.submit(()->compare(c9, hufProImage, hufOptImage));
						}

						boolean err = c0.get() != 0 || c1.get() != 0 || c2.get() != 0 || c3.get() != 0;

						String z = c0+" "+c1+" "+c2+" "+c3+" "+c4+" "+c5+" "+c6+" "+c7+" "+c8+" "+c9;

						System.out.printf("%-50s  %-5s  ari=%8d  ariProg=%8d  huff=%8d  huffProg=%8d  huffOpt=%8d  ariOverHuff=%8d (%6.3f)  %s%n", z, err?"ERROR":"OK", ariSeqData.size(), ariProData.size(), hufSeqData.size(), hufProData.size(), hufOptData.size(), hufProData.size()-ariProData.size(), 100*(hufProData.size()-ariProData.size())/(double)hufProData.size(), file.getAbsolutePath());

						totalAriLength += ariSeqData.size();
						totalAriProgLength += ariProData.size();
						totalHuffOptLength += hufOptData.size();
						totalHuffProgLength += hufProData.size();
						totalHuffLength += hufSeqData.size();

//						if(true) break;
					}
					catch (Throwable e)
					{
						System.out.println("------------------------------------------------------------------------------------------");
						System.out.println(file);
						e.printStackTrace(System.out);
						System.out.println("------------------------------------------------------------------------------------------");
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


	private static void process(byte[] aImageInData, ByteArrayOutputStream aImageOutData, File aFile, AtomicReference<BufferedImage> aImageOut, boolean aArithmetic, boolean aProgressive, boolean aOptimizedHuffman) throws IOException
	{
		new JPEGImageIO().setArithmetic(aArithmetic).setProgressive(aProgressive).setOptimizedHuffman(aOptimizedHuffman).transcode(new ByteArrayInputStream(aImageInData), aImageOutData);

		try (FileOutputStream fos = new FileOutputStream(aFile))
		{
			aImageOutData.writeTo(fos);
		}

		aImageOut.set(new JPEGImageIO().read(new ByteArrayInputStream(aImageOutData.toByteArray())));
	}


	private static void compare(AtomicInteger aResult, AtomicReference<BufferedImage> aImage1, AtomicReference<BufferedImage> aImage2)
	{
		BufferedImage img1 = aImage1.get();
		BufferedImage img2 = aImage2.get();

		int err = 0;
		for (int y = 0; y < img2.getHeight(); y++)
		{
			for (int x = 0; x < img2.getWidth(); x++)
			{
				if (img2.getRGB(x, y) != img1.getRGB(x, y))
				{
					err++;
				}
			}
		}

		aResult.set(err);
	}
}
