package examples;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class SingleImagePerformanceTest
{
	public static void main(String... args)
	{
		try
		{
			byte[] data = Files.readAllBytes(Paths.get("D:\\dev\\Image Compression Test Images\\earth.jpg"));
//			byte[] data = Files.readAllBytes(Paths.get("D:\\dev\\Lenna_444.jpg"));
//			byte[] data = Files.readAllBytes(Paths.get("D:\\dev\\Lenna_422.jpg"));
//			byte[] data = Files.readAllBytes(Paths.get("D:\\dev\\Lenna_420.jpg"));
//			byte[] data = Files.readAllBytes(Paths.get("D:\\dev\\Lenna_440.jpg"));
//			byte[] data = Files.readAllBytes(Paths.get("D:\\dev\\Lenna_411.jpg"));

//			for (int i = 0; i < 5; i++)
//			{
//				long t0 = System.nanoTime();
//				BufferedImage source = new JPEGImageIO().read(new ByteArrayInputStream(data));
//				long t1 = System.nanoTime();
//
//				System.out.println((t1 - t0) / 1000000.0);
//			}

			BufferedImage source = new JPEGImageIO().setLog(System.out).read(new ByteArrayInputStream(data));

			_ImageWindow.show(source);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
