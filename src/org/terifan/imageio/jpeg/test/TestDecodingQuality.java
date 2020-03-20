package org.terifan.imageio.jpeg.test;

import examples.*;
import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.SubsamplingMode;
import examples.res.R;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;


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
				.setSubsampling(SubsamplingMode._420) // Java ImageIO identical decoding: 444 440 411
				.write(src, output);

			BufferedImage javaImage = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
			BufferedImage teriImage = new JPEGImageIO().read(output.toByteArray());

			System.out.println(new _ImageQualityTest(src, javaImage, null));
			System.out.println(new _ImageQualityTest(src, teriImage, null));

			BufferedImage diffImage = new BufferedImage(javaImage.getWidth(), javaImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			for (int y = 0; y < javaImage.getHeight(); y++)
			{
				for (int x = 0; x < javaImage.getWidth(); x++)
				{
					int r0 = 0xff & (teriImage.getRGB(x, y) >> 16);
					int g0 = 0xff & (teriImage.getRGB(x, y) >> 8);
					int b0 = 0xff & (teriImage.getRGB(x, y) >> 0);
					int r1 = 0xff & (javaImage.getRGB(x, y) >> 16);
					int g1 = 0xff & (javaImage.getRGB(x, y) >> 8);
					int b1 = 0xff & (javaImage.getRGB(x, y) >> 0);

					if (r0==0&&g0==0&&b0==0)
					{
						diffImage.setRGB(x, y, 0x000000);
						continue;
					}

//					System.out.printf("%4d %4d, %4d %4d, %4d %4d%n", r0,r1,g0,g1,b0,b1);

					int d = r0 != r1 || g0 != g1 || b0 != b1 ? 0xffffff : 0x000000;
					diffImage.setRGB(x, y, d);
				}
			}

			_ImagePanel panel1 = new _ImagePanel().setImage(javaImage);
			_ImagePanel panel2 = new _ImagePanel().setImage(teriImage);
			_ImagePanel panel3 = new _ImagePanel().setImage(diffImage);
			panel1.setMirrorPanels(panel2, panel3);
			panel2.setMirrorPanels(panel1, panel3);
			panel3.setMirrorPanels(panel1, panel2);

			JFrame frame = new JFrame();
			frame.setLayout(new GridLayout(2, 2));
			frame.add(panel1);
			frame.add(panel2);
			frame.add(panel3);
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
}
