package examples;

import java.io.File;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.ProgressionScript;


public class TranscodeJPEGDemo
{
	public static void main(String... args)
	{
		try
		{
			for (File file : new File("C:\\dev\\Image Compression Suit").listFiles())
			{
				new JPEGImageIO()
					.setCompressionType(CompressionType.ArithmeticProgressive)
					.setQuality(95)
					.setProgressionScript(ProgressionScript.DC_THEN_AC)
					.setSubsampling(SubsamplingMode._444)
					.setFDCT(FDCTIntegerSlow.class)
//					.setLog(System.out)
					.write(ImageIO.read(file), new File("C:\\Terifan\\JPEGXL\\src\\test_images", file.getName().replace(".png", ".jpg")));
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


//	public static void main(String... args)
//	{
//		try
//		{
//			URL input = R.class.getResource("Swallowtail.jpg");
//
//			File output1 = new File("d:\\Swallowtail-arithmetic.jpg");
//			File output2 = new File("d:\\Swallowtail-arithmetic-progressive.jpg");
//			File output3 = new File("d:\\Swallowtail-huffman.jpg");
//			File output4 = new File("d:\\Swallowtail-huffman-optimized.jpg");
//			File output5 = new File("d:\\Swallowtail-huffman-progressive.jpg");
//
//			new JPEGImageIO().setCompressionType(CompressionType.Arithmetic).transcode(input, output1);
//			new JPEGImageIO().setCompressionType(CompressionType.ArithmeticProgressive).transcode(input, output2);
//			new JPEGImageIO().setCompressionType(CompressionType.Huffman).transcode(input, output3);
//			new JPEGImageIO().setCompressionType(CompressionType.HuffmanOptimized).transcode(input, output4);
//			new JPEGImageIO().setCompressionType(CompressionType.HuffmanProgressive).transcode(input, output5);
//
////			_ImageWindow.show(output1).setTitle("" + output1.length());
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}
}
