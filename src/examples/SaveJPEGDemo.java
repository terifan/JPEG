package examples;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import org.terifan.imageio.jpeg.encoder.ProgressionScript;
import examples.res.R;
import org.terifan.imageio.jpeg.CompressionType;


public class SaveJPEGDemo
{
	public static void main(String ... args)
	{
		try
		{
			URL input = R.class.getResource("Swallowtail-huff-opt-prog.jpg");

			BufferedImage myImage = new JPEGImageIO().read(input);

			File output = new File("d:\\Swallowtail-arithmetic.jpg");

			new JPEGImageIO()
				.setCompressionType(CompressionType.ArithmeticProgressive)
				.setQuality(95)
				.setProgressionScript(ProgressionScript.DC_THEN_AC)
				.setSubsampling(SubsamplingMode._444)
				.setLog(System.out)
				.write(myImage, output);

			myImage = new JPEGImageIO().read(output);

			_ImageWindow.show(myImage).setTitle("" + output.length());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
