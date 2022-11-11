package examples;

import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.JPEGImageIO;
import examples.res.R;


public class LoadJPEGDemo
{
	public static void main(String ... args)
	{
		try
		{
			BufferedImage myImage1 = new JPEGImageIO().read("D:\\Pictures\\Roliga bilder\\tshirt04.jpg");

//			BufferedImage myImage1 = new JPEGImageIO().read(R.class.getResource("Swallowtail-huff-def.jpg"));
//			BufferedImage myImage2 = new JPEGImageIO().read(R.class.getResource("Swallowtail-huff-opt.jpg"));
//			BufferedImage myImage3 = new JPEGImageIO().read(R.class.getResource("Swallowtail-huff-opt-prog.jpg"));
//			BufferedImage myImage4 = new JPEGImageIO().read(R.class.getResource("Swallowtail-ari.jpg"));
//			BufferedImage myImage5 = new JPEGImageIO().read(R.class.getResource("Swallowtail-ari-prog.jpg"));

//			BufferedImage myImage = new JPEGImageIO().setIDCT(IDCTIntegerSlow.class).read(R.class.getResource("Swallowtail-ari-prog.jpg"));

			_ImageWindow.show(myImage1);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
