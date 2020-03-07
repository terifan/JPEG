package org.terifan.imageio.jpeg.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class ImageFrame extends JFrame
{
	private BufferedImage mImage;


	private ImageFrame()
	{
	}


	public static ImageFrame show(File aFile) throws IOException
	{
		ImageFrame imageFrame = new ImageFrame();
		imageFrame.mImage = new JPEGImageIO().read(aFile);
//		imageFrame.mImage = ImageIO.read(aFile);
		imageFrame.display();

		return imageFrame;
	}


	public static ImageFrame show(BufferedImage aImage)
	{
		ImageFrame imageFrame = new ImageFrame();
		imageFrame.mImage = aImage;
		imageFrame.display();

		return imageFrame;
	}


	public ImageFrame setImage(BufferedImage aImage)
	{
		mImage = aImage;
		repaint();
		return this;
	}


	private void display()
	{
		add(new JPanel()
		{
			@Override
			protected void paintComponent(Graphics aGraphics)
			{
				int w = getWidth();
				int h = getHeight();

				aGraphics.setColor(Color.WHITE);
				aGraphics.fillRect(0, 0, w, h);
				aGraphics.setColor(Color.LIGHT_GRAY);
				for (int y = 0; y < h; y+=20)
				{
					for (int x = y % 40; x < w; x+=40)
					{
						aGraphics.fillRect(x, y, 20, 20);
					}
				}
				if (mImage != null)
				{
					aGraphics.drawImage(mImage, (w-mImage.getWidth())/2, (h-mImage.getHeight())/2, null);
				}
			}

			@Override
			public Dimension getPreferredSize()
			{
				if (mImage == null)
				{
					return new Dimension(1024,768);
				}
				return new Dimension(mImage.getWidth(), mImage.getHeight());
			}
		});
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
