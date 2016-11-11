package org.terifan.multimedia.jpeg.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class ImagePane extends JFrame
{
	private BufferedImage mImage;
	public ImagePane()
	{
		add(new JPanel()
		{
			@Override
			protected void paintComponent(Graphics aGraphics)
			{
				if (mImage != null)
				{
					aGraphics.drawImage(mImage, 0, 0, null);
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
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setSize(1024, 768);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}


	public ImagePane setImage(BufferedImage aImage)
	{
		mImage = aImage;
		return this;
	}
}
