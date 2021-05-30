package org.terifan.imageio.jpeg.test;

import examples._ImageWindow;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.JPEGImageIOException;


public class TestTranscode2
{
	public static void main(String... args)
	{
		try
		{
			File file = new File("D:/dev/[broken]/autumn-landscape-wallpaper-1920x1080-1008099.jpg");
//			File file = new File("D:/Pictures/3gupyxzeq0v21.jpg");

			BufferedImage myImage = new JPEGImageIO().setLog(System.out).read(file);
			_ImageWindow.show(myImage);

//			processFile(file);

//			process("D:\\dev\\[broken]");

//			process("D:\\Pictures");
//			process("R:\\Collections");
//			process("R:\\Picture Sets");
//			process("R:\\Pictures By Site");
//			process("R:\\Pictures Favorites");
//			process("R:\\Pictures High Quality");
//			process("R:\\Pictures Temp");
//			process("O:\\Pictures Incoming");
//			process("O:\\Pictures Unsorted");
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void process(String aPath) throws IOException
	{
		Files.walk(Paths.get(aPath)).filter(p->p.getFileName().toString().matches("(i?).*jpg")).parallel().forEach(p ->
		{
			File file = p.toFile();

			try
			{
				processFile(file);
			}
			catch (Throwable e)
			{
				System.out.printf("%-10s %10d %10d %10d %s%n", "Exception", file.length(), 0, 0, file);

				copy(file);
			}
		});
	}


	private static void processFile(File aFile) throws JPEGImageIOException, IOException
	{
		ByteArrayOutputStream ariImage = new ByteArrayOutputStream();

		new JPEGImageIO()
			.setLog(System.out)
			.setCompressionType(CompressionType.ArithmeticProgressive).transcode(aFile, ariImage);

		ByteArrayOutputStream hufImage = new ByteArrayOutputStream();

		new JPEGImageIO().setCompressionType(CompressionType.HuffmanOptimized).transcode(ariImage.toByteArray(), hufImage);

		BufferedImage image1 = ImageIO.read(new ByteArrayInputStream(hufImage.toByteArray()));
		BufferedImage image2 = ImageIO.read(aFile);

		double err = MeasureErrorRate.measureError(image1, image2);

		System.out.printf("%-10s %10d %10d %10d %s%n", err>0?"Bad pixels":"", aFile.length(), ariImage.size(), hufImage.size(), aFile);

		if (err>0)
		{
			copy(aFile);
		}
//		else
//			aFile.delete();
	}


	private static void copy(File aFile)
	{
		try
		{
			File dst = new File("D:\\dev\\[broken]", aFile.getName());

			if (!dst.exists())
			{
				byte[] buf = new byte[(int)aFile.length()];

				try (DataInputStream in = new DataInputStream(new FileInputStream(aFile));final FileOutputStream out = new FileOutputStream(dst))
				{
					in.readFully(buf);
					out.write(buf);
				}
			}
		}
		catch (Exception e)
		{
		}
	}
}
