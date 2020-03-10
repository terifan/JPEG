package examples;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;


public class PerformanceTest
{
	public static void main(String... args)
	{
		try
		{
			System.out.printf("%-20s %10s %10s %10s %10s %10s %10s%n", "", "Dec Java", "Dec Teri", "Enc Java", "Enc Teri", "Dec Java", "Dec Teri");

			for (File file : new File("D:\\dev\\Image Compression Test Images\\8K").listFiles())
			{
				byte[] data = Files.readAllBytes(file.toPath());

				long t0 = System.nanoTime();
				BufferedImage source1 = ImageIO.read(new ByteArrayInputStream(data));
				long t1 = System.nanoTime();

				long t2 = System.nanoTime();
				BufferedImage source2 = new JPEGImageIO().read(new ByteArrayInputStream(data));
				long t3 = System.nanoTime();

				ByteArrayOutputStream output1 = new ByteArrayOutputStream();
				ByteArrayOutputStream output2 = new ByteArrayOutputStream();

				long t4 = System.nanoTime();
				ImageIO.write(source1, "jpg", output2);
				long t5 = System.nanoTime();

				long t6 = System.nanoTime();
				new JPEGImageIO().setSubsampling(SubsamplingMode._444).setCompressionType(CompressionType.Huffman).setQuality(75).write(source1, output1);
				long t7 = System.nanoTime();

				long t8 = System.nanoTime();
				BufferedImage source3 = ImageIO.read(new ByteArrayInputStream(output1.toByteArray()));
				long t9 = System.nanoTime();

				long t10 = System.nanoTime();
				BufferedImage source4 = new JPEGImageIO().read(new ByteArrayInputStream(output1.toByteArray()));
				long t11 = System.nanoTime();

				System.out.printf("%20s %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f%n", file.getName(), (t1 - t0) / 1000000.0, (t3 - t2) / 1000000.0, (t5 - t4) / 1000000.0, (t7 - t6) / 1000000.0, (t9 - t8) / 1000000.0, (t11 - t10) / 1000000.0);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
