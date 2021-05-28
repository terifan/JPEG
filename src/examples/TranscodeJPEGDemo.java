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
//			URL input = R.class.getResource("Swallowtail.jpg");
			File input = new File("D:\\Pictures\\Wallpapers Fantasy\\dragon-wallpaper-1920x1080-1009013.jpg");

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
