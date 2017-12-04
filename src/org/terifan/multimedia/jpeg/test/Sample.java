package org.terifan.multimedia.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.terifan.multimedia.jpeg.JPEGImageReader;


public class Sample
{
	public static void main(String... args)
	{
		try
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new FileNameExtensionFilter("JPEG file", new String[] {"jpg", "jpeg"}));

			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(chooser.getSelectedFile())))
				{
					long t = System.currentTimeMillis();
					BufferedImage image = JPEGImageReader.read(input);
					t = System.currentTimeMillis() - t;

					ImageFrame imagePane = new ImageFrame().setImage(image);
					imagePane.setTitle(chooser.getSelectedFile() + " [" + t + " ms, " + image.getWidth() + "x" + image.getHeight() + "]");
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}