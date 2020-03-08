package examples;

import java.io.File;
import java.net.URL;
import org.terifan.imageio.jpeg.JPEGImageIO;
import examples.res.R;
import org.terifan.imageio.jpeg.CompressionType;


public class TranscodeJPEGDemo
{
	public static void main(String... args)
	{
		try
		{
			URL input = R.class.getResource("Swallowtail.jpg");

			File output = new File("d:\\Swallowtail-arithmetic.jpg");

			new JPEGImageIO().setCompressionType(CompressionType.ArithmeticProgressive).transcode(input, output);

			_ImageWindow.show(output).setTitle("" + output.length());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
