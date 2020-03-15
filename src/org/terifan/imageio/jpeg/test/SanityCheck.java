package org.terifan.imageio.jpeg.test;

import examples.*;
import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import examples.res.R;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.JPEGImageIOException;
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
			JFrame frame = new JFrame();
			frame.setLayout(new GridLayout(2, 5, 10, 10));

			BufferedImage src = ImageIO.read(R.class.getResource("Lenna.png"));
			src = src.getSubimage(20, 110, 443, 313); // prime number width/height = uneven mcu size
			run(src, frame);

			src = ImageIO.read(R.class.getResource("LennaGray.png"));
			src = src.getSubimage(20, 110, 443, 313); // prime number width/height = uneven mcu size
			run(src, frame);

			frame.setSize(2400, 840);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void run(BufferedImage aSrc, JFrame aFrame) throws JPEGImageIOException, IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new JPEGImageIO().setCompressionType(CompressionType.Huffman).setFDCT(FDCTFloat.class).setQuality(100).setSubsampling(SubsamplingMode._411).write(aSrc, output);
		new JPEGImageIO().setCompressionType(CompressionType.HuffmanOptimized).setFDCT(FDCTIntegerFast.class).setQuality(100).setSubsampling(SubsamplingMode._420).write(aSrc, output);
		new JPEGImageIO().setCompressionType(CompressionType.Arithmetic).setFDCT(FDCTIntegerSlow.class).setQuality(100).setSubsampling(SubsamplingMode._440).write(aSrc, output);
		new JPEGImageIO().setCompressionType(CompressionType.HuffmanProgressive).setQuality(100).setSubsampling(SubsamplingMode._422).write(aSrc, output);
		new JPEGImageIO().setCompressionType(CompressionType.ArithmeticProgressive).setQuality(100).setSubsampling(SubsamplingMode._444).write(aSrc, output);

		ByteArrayInputStream in = new ByteArrayInputStream(output.toByteArray());
		BufferedImage image1 = new JPEGImageIO().setIDCT(IDCTIntegerSlow.class).read(in);
		BufferedImage image2 = new JPEGImageIO().setIDCT(IDCTFloat.class).read(in);
		BufferedImage image3 = new JPEGImageIO().setIDCT(IDCTIntegerFast.class).read(in);
		BufferedImage image4 = new JPEGImageIO().read(in);
		BufferedImage image5 = new JPEGImageIO().read(in);

		BufferedImage imageJava = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));

		add(image1, imageJava, aFrame, "H 4:1:1");
		add(image2, imageJava, aFrame, "HO 4:2:0");
		add(image3, imageJava, aFrame, "A 4:4:0");
		add(image4, imageJava, aFrame, "HP 4:2:2");
		add(image5, imageJava, aFrame, "AP 4:4:4");
	}


	private static void add(BufferedImage aImage, BufferedImage aImageJava, JFrame aFrame, String aDescription)
	{
		if (aImage != null)
		{
			_ImagePanel imagePanel = new _ImagePanel().setImage(aImage);

			JLabel label = new JLabel(aDescription + " " + new _ImageQualityTest(aImage, aImageJava, null).toString());
			label.setHorizontalAlignment(SwingConstants.CENTER);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(label, BorderLayout.NORTH);
			panel.add(imagePanel, BorderLayout.CENTER);

			aFrame.add(panel);
		}
	}
}
