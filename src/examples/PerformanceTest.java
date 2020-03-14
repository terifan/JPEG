package examples;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import org.terifan.imageio.jpeg.test._JavaJPEGEncoder;


public class PerformanceTest
{
	public static void main(String... args)
	{
		try
		{
			System.out.printf("%10s %10s %10s %10s %10s %10s  %s%n", "Dec Java", "Dec Teri", "Enc Java", "Enc Teri", "Dec Java", "Dec Teri", "");

			for (int iteration = 0; iteration < 1; iteration++)
			{
//				for (File file : new File("D:\\dev\\Image Compression Test Images\\8K").listFiles())
//				for (File file : new File("c:\\Pictures\\Wallpapers High Quality").listFiles(e->e.getName().toLowerCase().endsWith(".jpg")))
				for (File file : new File[]{new File("D:\\dev\\Image Compression Test Images\\earth.jpg")})
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
					_JavaJPEGEncoder.write(source2, output2, 90, SubsamplingMode._440);
					long t5 = System.nanoTime();

					System.out.println(new JPEGImageIO().decode(output2.toByteArray()).mSOFSegment.getSubsamplingMode());

					_ImageWindow.show(new JPEGImageIO().read(output2.toByteArray()));

					long t6 = System.nanoTime();
					new JPEGImageIO().setSubsampling(SubsamplingMode._420).setCompressionType(CompressionType.Huffman).setQuality(90).write(source1, output1);
					long t7 = System.nanoTime();

					long t8 = System.nanoTime();
					BufferedImage source3 = ImageIO.read(new ByteArrayInputStream(output1.toByteArray()));
					long t9 = System.nanoTime();

					long t10 = System.nanoTime();
					BufferedImage source4 = new JPEGImageIO().read(new ByteArrayInputStream(output1.toByteArray()));
					long t11 = System.nanoTime();

					System.out.printf("%10.2f %10.2f %10.2f %10.2f %10.2f %10.2f  %s%n", (t1 - t0) / 1000000.0, (t3 - t2) / 1000000.0, (t5 - t4) / 1000000.0, (t7 - t6) / 1000000.0, (t9 - t8) / 1000000.0, (t11 - t10) / 1000000.0, file.getName());
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
