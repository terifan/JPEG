package org.terifan.imageio.jpeg.test;

import examples.*;
import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import examples.res.R;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;


public class TestDecodingQuality
{
	public static void main(String... args)
	{
		try
		{
			BufferedImage src = ImageIO.read(R.class.getResource("Lenna.png"));

			ByteArrayOutputStream output = new ByteArrayOutputStream();

			new JPEGImageIO()
				.setCompressionType(CompressionType.Huffman)
				.setQuality(100)
//				.setFDCT(FDCTFloat.class)
//				.setFDCT(FDCTIntegerFast.class)
//				.setFDCT(FDCTIntegerSlow.class)
//				.setSubsampling(SubsamplingMode._420) // Java ImageIO identical decoding: 444 440 411
				.setSubsampling(SubsamplingMode._444) // Java ImageIO identical decoding: 444 440 411
				.write(src, output);

			BufferedImage javaImage = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
			BufferedImage teriImage = new JPEGImageIO().read(output.toByteArray());

			System.out.println("original/java: " + new _ImageQualityTest(src, javaImage, null));
			System.out.println("original/teri: " + new _ImageQualityTest(src, teriImage, null));
			System.out.println("    java/teri: " + new _ImageQualityTest(javaImage, teriImage, null));

			AtomicInteger errorJava = new AtomicInteger();
			AtomicInteger errorTeri = new AtomicInteger();
			BufferedImage diffJava = createDiffImage(src, javaImage, errorJava);
			BufferedImage diffTeri = createDiffImage(src, teriImage, errorTeri);

			_ImagePanel panel1 = new _ImagePanel().setImage(javaImage).setLabel("Java");
			_ImagePanel panel2 = new _ImagePanel().setImage(teriImage).setLabel("Teri");
			_ImagePanel panel3 = new _ImagePanel().setImage(diffJava).setLabel("Java");
			_ImagePanel panel4 = new _ImagePanel().setImage(diffTeri).setLabel("Teri");
			panel1.setMirrorPanels(panel2, panel3, panel4);
			panel2.setMirrorPanels(panel1, panel3, panel4);
			panel3.setMirrorPanels(panel1, panel2, panel4);
			panel4.setMirrorPanels(panel1, panel2, panel3);

			JFrame frame = new JFrame("error java=" + errorJava+", teri="+errorTeri);
			frame.setLayout(new GridLayout(2, 2));
			frame.add(panel1);
			frame.add(panel2);
			frame.add(panel3);
			frame.add(panel4);
			frame.setSize(1455, 1240);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static BufferedImage createDiffImage(BufferedImage aJavaImage, BufferedImage aTeriImage, AtomicInteger aError)
	{
		BufferedImage diffImage = new BufferedImage(aJavaImage.getWidth(), aJavaImage.getHeight(), BufferedImage.TYPE_INT_RGB);

		int errMax = 0;

		for (int z = 0; z < 2; z++)
		{
			for (int y = 0; y < aJavaImage.getHeight(); y++)
			{
				for (int x = 0; x < aJavaImage.getWidth(); x++)
				{
					int c0 = aTeriImage.getRGB(x, y);
					int c1 = aJavaImage.getRGB(x, y);

					int dr = Math.abs((0xff & (c0 >> 16)) - (0xff & (c1 >> 16)));
					int dg = Math.abs((0xff & (c0 >> 8)) - (0xff & (c1 >> 8)));
					int db = Math.abs((0xff & (c0 >> 0)) - (0xff & (c1 >> 0)));

					if (z == 0)
					{
						errMax = Math.max(errMax, dr);
						errMax = Math.max(errMax, dg);
						errMax = Math.max(errMax, db);
					}
					else
					{
						dr = dr * 255 / errMax;
						dg = dg * 255 / errMax;
						db = db * 255 / errMax;
						diffImage.setRGB(x, y, (dr << 16) + (dg << 8) + db);
					}
				}
			}

			if (errMax == 0) break;
		}

		aError.set(errMax);

		return diffImage;
	}
}
