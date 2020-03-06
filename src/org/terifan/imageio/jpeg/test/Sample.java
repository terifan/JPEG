package org.terifan.imageio.jpeg.test;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;


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
				ImageFrame.show(chooser.getSelectedFile());
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}