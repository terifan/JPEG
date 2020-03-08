package examples;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class _ImageWindow extends JFrame
{
	private _ImagePanel mImagePanel;


	private _ImageWindow()
	{
		mImagePanel = new _ImagePanel();
	}


	public static _ImageWindow show(File aFile) throws IOException
	{
		_ImageWindow imageFrame = new _ImageWindow();
		imageFrame.setImage(new JPEGImageIO().read(aFile));
		imageFrame.display();

		return imageFrame;
	}


	public static _ImageWindow show(BufferedImage aImage)
	{
		_ImageWindow imageFrame = new _ImageWindow();
		imageFrame.setImage(aImage);
		imageFrame.display();

		return imageFrame;
	}


	public _ImageWindow setImage(BufferedImage aImage)
	{
		mImagePanel.setImage(aImage);
		mImagePanel.repaint();
		return this;
	}


	private void display()
	{
		add(mImagePanel);
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
