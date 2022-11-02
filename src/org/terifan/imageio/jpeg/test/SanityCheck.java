package org.terifan.imageio.jpeg.test;

import examples.*;
import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import examples.res.R;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
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
		ByteArrayOutputStream output411 = new ByteArrayOutputStream();
		ByteArrayOutputStream output420 = new ByteArrayOutputStream();
		ByteArrayOutputStream output440 = new ByteArrayOutputStream();
		ByteArrayOutputStream output422 = new ByteArrayOutputStream();
		ByteArrayOutputStream output444 = new ByteArrayOutputStream();

		BufferedImage javaImage411 = getJavaDecodedImage(aSrc, SubsamplingMode._411);
		BufferedImage javaImage420 = getJavaDecodedImage(aSrc, SubsamplingMode._420);
		BufferedImage javaImage440 = getJavaDecodedImage(aSrc, SubsamplingMode._440);
		BufferedImage javaImage422 = getJavaDecodedImage(aSrc, SubsamplingMode._422);
		BufferedImage javaImage444 = getJavaDecodedImage(aSrc, SubsamplingMode._444);

		new JPEGImageIO().setCompressionType(CompressionType.Huffman).setQuality(100).setSubsampling(SubsamplingMode._411).write(aSrc, output411);
		new JPEGImageIO().setCompressionType(CompressionType.HuffmanOptimized).setQuality(100).setSubsampling(SubsamplingMode._420).write(aSrc, output420);
		new JPEGImageIO().setCompressionType(CompressionType.Arithmetic).setQuality(100).setSubsampling(SubsamplingMode._440).write(aSrc, output440);
		new JPEGImageIO().setCompressionType(CompressionType.HuffmanProgressive).setQuality(100).setSubsampling(SubsamplingMode._422).write(aSrc, output422);
		new JPEGImageIO().setCompressionType(CompressionType.ArithmeticProgressive).setQuality(100).setSubsampling(SubsamplingMode._444).write(aSrc, output444);

		BufferedImage teriImage411 = new JPEGImageIO().read(new ByteArrayInputStream(output411.toByteArray()));
		BufferedImage teriImage420 = new JPEGImageIO().read(new ByteArrayInputStream(output420.toByteArray()));
		BufferedImage teriImage440 = new JPEGImageIO().read(new ByteArrayInputStream(output440.toByteArray()));
		BufferedImage teriImage422 = new JPEGImageIO().read(new ByteArrayInputStream(output422.toByteArray()));
		BufferedImage teriImage444 = new JPEGImageIO().read(new ByteArrayInputStream(output444.toByteArray()));

		add(teriImage411, javaImage411, aFrame, "H 4:1:1");
		add(teriImage420, javaImage420, aFrame, "HO 4:2:0");
		add(teriImage440, javaImage440, aFrame, "A 4:4:0");
		add(teriImage422, javaImage422, aFrame, "HP 4:2:2");
		add(teriImage444, javaImage444, aFrame, "AP 4:4:4");
	}


	private static BufferedImage getJavaDecodedImage(BufferedImage aSrc, SubsamplingMode aSubsamplingMode) throws JPEGImageIOException, IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new JPEGImageIO().setQuality(100).setSubsampling(aSubsamplingMode).write(aSrc, output);
		return ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
	}


	private static void add(BufferedImage aImage, BufferedImage aImageJava, JFrame aFrame, String aDescription)
	{
		if (aImage != null)
		{
			_ImagePanel imagePanel = new _ImagePanel().setImage(aImage);
			_ImageQualityTest test = new _ImageQualityTest(aImage, aImageJava, null);

			JLabel label = new JLabel(aDescription + " " + test.toString());
			label.setForeground(test.psnr != 0 ? Color.RED : Color.BLACK);
			label.setHorizontalAlignment(SwingConstants.CENTER);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(label, BorderLayout.NORTH);
			panel.add(imagePanel, BorderLayout.CENTER);

			aFrame.add(panel);
		}
	}
}
