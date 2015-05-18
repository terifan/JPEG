package org.terifan.multimedia.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import javax.swing.JFrame;
import org.terifan.multimedia.jpeg.JPEGImageReader;
import org.terifan.ui.ImagePane;
import org.terifan.util.log.Log;


public class ThumbDecoder
{
	public static void main(String[] args)
	{
		try
		{
			BufferedImage image = JPEGImageReader.read(new FileInputStream("D:\\Pictures\\Girls\\Avril Lavigne\\2004.09.07 Fashion Rocks.jpg"));

			JFrame frame = new JFrame();
			frame.add(new ImagePane(image));
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		catch (Exception e)
		{
			e.printStackTrace(Log.out);
			System.exit(0);
		}
	}
}