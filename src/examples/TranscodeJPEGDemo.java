package examples;

import examples.res.R;
import java.io.File;
import java.net.URL;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class TranscodeJPEGDemo
{
	public static void main(String... args)
	{
		try
		{
			URL input = R.class.getResource("Swallowtail.jpg");

			File output1 = new File("d:\\Swallowtail-arithmetic.jpg");
			File output2 = new File("d:\\Swallowtail-arithmetic-progressive.jpg");
			File output3 = new File("d:\\Swallowtail-huffman.jpg");
			File output4 = new File("d:\\Swallowtail-huffman-optimized.jpg");
			File output5 = new File("d:\\Swallowtail-huffman-progressive.jpg");

			new JPEGImageIO().setCompressionType(CompressionType.Arithmetic).transcode(input, output1);
			new JPEGImageIO().setCompressionType(CompressionType.ArithmeticProgressive).transcode(input, output2);
			new JPEGImageIO().setCompressionType(CompressionType.Huffman).transcode(input, output3);
			new JPEGImageIO().setCompressionType(CompressionType.HuffmanOptimized).transcode(input, output4);
			new JPEGImageIO().setCompressionType(CompressionType.HuffmanProgressive).transcode(input, output5);

			_ImageWindow.show(output1).setTitle("" + output1.length());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
