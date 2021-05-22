package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIO;


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

			for (File dir : new File("d:\\").listFiles(e->e.isDirectory()))
			{
				for (File file : dir.listFiles(e->e.getName().toLowerCase().endsWith(".jpg")))
//				for (File file : new File("D:\\Pictures\\Wallpapers High Quality").listFiles(e->e.getName().toLowerCase().endsWith(".jpg")))
//				for (File file : new File("C:\\Pictures\\Wallpapers High Quality").listFiles(e->e.getName().toLowerCase().endsWith(".jpg")))
				{
					byte[] data = Files.readAllBytes(file.toPath());

					try
					{
						ByteArrayOutputStream ariSeqData = new ByteArrayOutputStream();
						ByteArrayOutputStream ariProData = new ByteArrayOutputStream();
						ByteArrayOutputStream hufProData = new ByteArrayOutputStream();
						ByteArrayOutputStream hufOptData = new ByteArrayOutputStream();
						ByteArrayOutputStream hufSeqData = new ByteArrayOutputStream();

						BufferedImage ariSeqImage = process(data, ariSeqData, new File(new File(out, "jpg-ari"), file.getName()), CompressionType.Arithmetic);
						BufferedImage ariProImage = process(data, ariProData, new File(new File(out, "jpg-ari-prog"), file.getName()), CompressionType.ArithmeticProgressive);
						BufferedImage hufSeqImage = process(data, hufSeqData, new File(new File(out, "jpg-huff"), file.getName()), CompressionType.Huffman);
						BufferedImage hufOptImage = process(data, hufOptData, new File(new File(out, "jpg-huff-opt"), file.getName()), CompressionType.HuffmanOptimized);
						BufferedImage hufProImage = process(data, hufProData, new File(new File(out, "jpg-huff-prog"), file.getName()), CompressionType.HuffmanProgressive);

						int c0 = compare(ariSeqImage, ariProImage);
						int c1 = compare(ariSeqImage, hufSeqImage);
						int c2 = compare(ariSeqImage, hufOptImage);
						int c3 = compare(ariSeqImage, hufProImage);

						boolean err = c0 != 0 || c1 != 0 || c2 != 0 || c3 != 0;

						System.out.printf("%-5s  ari=%8d  ariProg=%8d  huff=%8d  huffProg=%8d  huffOpt=%8d  ariOverHuff=%8d (%6.3f)  %s%n", err?"ERROR":"OK", ariSeqData.size(), ariProData.size(), hufSeqData.size(), hufProData.size(), hufOptData.size(), hufProData.size()-ariProData.size(), 100*(hufProData.size()-ariProData.size())/(double)hufProData.size(), file.getAbsolutePath());

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


	private static BufferedImage process(byte[] aImageInData, ByteArrayOutputStream aImageOutData, File aFile, CompressionType aCompressionType) throws IOException
	{
		new JPEGImageIO().setCompressionType(aCompressionType).transcode(new ByteArrayInputStream(aImageInData), aImageOutData);

		try (FileOutputStream fos = new FileOutputStream(aFile))
		{
			aImageOutData.writeTo(fos);
		}

		return new JPEGImageIO().read(new ByteArrayInputStream(aImageOutData.toByteArray()));
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
