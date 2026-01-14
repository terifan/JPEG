package examples;

import java.io.File;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class TranscodeJPEGDemo
{
	public static void main(String... args)
	{
		try
		{
			String filename = "4k-3840-x-2160-wallpapers-themefoxx (113).jpg";

			File input = new File("D:\\Home\\Pictures\\Wallpapers\\" + filename);

			File dest = new File("D:\\data\\");

			File output1 = new File(dest, "jpg-ari\\" + filename);
			File output2 = new File(dest, "jpg-ari-prog\\" + filename);
			File output3 = new File(dest, "jpg-huff\\" + filename);
			File output4 = new File(dest, "jpg-huff-opt\\" + filename);
			File output5 = new File(dest, "jpg-huff-prog\\" + filename);

			new JPEGImageIO().setLog(System.out).setCompressionType(CompressionType.Arithmetic).transcode(input, output1);
			new JPEGImageIO().setCompressionType(CompressionType.ArithmeticProgressive).transcode(input, output2);
			new JPEGImageIO().setCompressionType(CompressionType.Huffman).transcode(input, output3);
			new JPEGImageIO().setCompressionType(CompressionType.HuffmanOptimized).transcode(input, output4);
			new JPEGImageIO().setCompressionType(CompressionType.HuffmanProgressive).transcode(input, output5);

//			_ImageWindow.show(output1).setTitle("" + output1.length());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
