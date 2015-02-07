package org.terifan.multimedia.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.terifan.multimedia.jpeg.JPEGImageReader;
import org.terifan.ui.ImagePane;
import org.terifan.util.StopWatch;


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
				StopWatch stopWatch = new StopWatch();
				BufferedInputStream input = new BufferedInputStream(new FileInputStream(chooser.getSelectedFile()));
				BufferedImage image = JPEGImageReader.read(input);
				stopWatch.stop();

				JFrame frame = new JFrame(chooser.getSelectedFile() + " [" + stopWatch + ", " + image.getWidth() + "x" + image.getHeight() + "]");
				frame.add(new ImagePane(image));
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
				frame.setVisible(true);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}