package org.terifan.imageio.jpeg.test;

import examples.*;
import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import examples.res.R;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;


public class SanityCheck
{
	public static void main(String... args)
	{
		try
		{
			BufferedImage src = ImageIO.read(R.class.getResource("Lenna.png"));
			src = src.getSubimage(20, 100, 443, 313); // prime number width/height = uneven mcu size

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			new JPEGImageIO().setCompressionType(CompressionType.Huffman).setFDCT(FDCTFloat.class).setQuality(95).setSubsampling(SubsamplingMode._411).write(src, output);
			new JPEGImageIO().setCompressionType(CompressionType.HuffmanOptimized).setFDCT(FDCTIntegerFast.class).setQuality(95).setSubsampling(SubsamplingMode._420).write(src, output);
			new JPEGImageIO().setCompressionType(CompressionType.Arithmetic).setFDCT(FDCTIntegerSlow.class).setQuality(95).setSubsampling(SubsamplingMode._440).write(src, output);
			new JPEGImageIO().setCompressionType(CompressionType.HuffmanProgressive).setQuality(95).setSubsampling(SubsamplingMode._422).write(src, output);
			new JPEGImageIO().setCompressionType(CompressionType.ArithmeticProgressive).setQuality(95).setSubsampling(SubsamplingMode._444).write(src, output);

			ByteArrayInputStream in = new ByteArrayInputStream(output.toByteArray());
			BufferedImage image1 = new JPEGImageIO().setIDCT(IDCTIntegerSlow.class).read(in);
			BufferedImage image2 = new JPEGImageIO().setIDCT(IDCTFloat.class).read(in);
			BufferedImage image3 = new JPEGImageIO().setIDCT(IDCTIntegerFast.class).read(in);
			BufferedImage image4 = new JPEGImageIO().read(in);
			BufferedImage image5 = new JPEGImageIO().read(in);

			BufferedImage imageJ = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));

			if (image1 != null) System.out.println(new _ImageQualityTest(image1, imageJ, null).psnr);
			if (image2 != null) System.out.println(new _ImageQualityTest(image2, imageJ, null).psnr);
			if (image3 != null) System.out.println(new _ImageQualityTest(image3, imageJ, null).psnr);
			if (image4 != null) System.out.println(new _ImageQualityTest(image4, imageJ, null).psnr);
			if (image5 != null) System.out.println(new _ImageQualityTest(image5, imageJ, null).psnr);

			_ImagePanel panel1 = new _ImagePanel().setImage(image1);
			_ImagePanel panel2 = new _ImagePanel().setImage(image2);
			_ImagePanel panel3 = new _ImagePanel().setImage(image3);
			_ImagePanel panel4 = new _ImagePanel().setImage(image4);
			_ImagePanel panel5 = new _ImagePanel().setImage(image5);

			JFrame frame = new JFrame();
			frame.setLayout(new GridLayout(2, 3));
			frame.add(panel1);
			frame.add(panel2);
			frame.add(panel3);
			frame.add(panel4);
			frame.add(panel5);
			frame.setSize(1455, 840);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
