package org.terifan.imageio.jpeg.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class ImageFrame extends JFrame
{
	private BufferedImage mImage;


	public ImageFrame()
	{
		this(null);
	}


	public ImageFrame(BufferedImage aImage)
	{
		mImage = aImage;

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


	public ImageFrame setImage(BufferedImage aImage)
	{
		mImage = aImage;
		return this;
	}
}
