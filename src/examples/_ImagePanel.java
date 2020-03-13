package examples;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;


public class _ImagePanel extends JPanel implements MouseListener, MouseWheelListener, MouseMotionListener
{
	private BufferedImage mImage;
	private boolean mMouseButtonPressed;
	private int mStartX;
	private int mStartY;
	private int mOffsetX;
	private int mOffsetY;
	private int mOldOffsetX;
	private int mOldOffsetY;
	private double mScale;
	private _ImagePanel[] mMirrorPanels;


	public _ImagePanel()
	{
		mScale = 1;
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}


	public void setMirrorPanels(_ImagePanel... aPanels)
	{
		mMirrorPanels = aPanels;
	}


	@Override
	protected void paintComponent(Graphics aGraphics)
	{
		int w = getWidth();
		int h = getHeight();

		aGraphics.setColor(Color.WHITE);
		aGraphics.fillRect(0, 0, w, h);
		aGraphics.setColor(Color.LIGHT_GRAY);
		for (int y = 0; y < h; y += 20)
		{
			for (int x = y % 40; x < w; x += 40)
			{
				aGraphics.fillRect(x, y, 20, 20);
			}
		}

		if (mImage != null)
		{
			int iw = (int)(mScale * mImage.getWidth());
			int ih = (int)(mScale * mImage.getHeight());

			int x = (int)(0.5 * getWidth() - 0.5 * iw + mOffsetX);
			int y = (int)(0.5 * getHeight() - 0.5 * ih + mOffsetY);

			aGraphics.drawImage(mImage, x, y, iw, ih, null);
		}
	}


	@Override
	public Dimension getPreferredSize()
	{
		if (mImage == null)
		{
			return new Dimension(256, 256);
		}
		return new Dimension(mImage.getWidth(), mImage.getHeight());
	}


	public BufferedImage getImage()
	{
		return mImage;
	}


	public _ImagePanel setImage(BufferedImage aImage)
	{
		mImage = aImage;
		return this;
	}


	@Override
	public void mouseClicked(MouseEvent aEvent)
	{
	}


	@Override
	public void mousePressed(MouseEvent aEvent)
	{
		requestFocus();
		mMouseButtonPressed = true;
		mStartX = mOffsetX;
		mStartY = mOffsetY;
		mOldOffsetX = aEvent.getX();
		mOldOffsetY = aEvent.getY();
	}


	@Override
	public void mouseReleased(MouseEvent aEvent)
	{
		mMouseButtonPressed = false;
	}


	@Override
	public void mouseEntered(MouseEvent aEvent)
	{
	}


	@Override
	public void mouseExited(MouseEvent aEvent)
	{
	}


	@Override
	public void mouseWheelMoved(MouseWheelEvent aEvent)
	{
		int w = mImage.getWidth();
		int h = mImage.getHeight();

		int scroll = aEvent.getUnitsToScroll() < 0 ? -1 : 1;

		double px = super.getWidth() / 2.0 + mOffsetX - mScale * w / 2.0;
		double py = super.getHeight() / 2.0 + mOffsetY - mScale * h / 2.0;
		double pw = mScale * w;
		double ph = mScale * h;

		double mx = Math.min(Math.max(aEvent.getX(), px), px + pw);
		double my = Math.min(Math.max(aEvent.getY(), py), py + ph);

		int dx = (int)(mx - super.getWidth() / 2.0);
		int dy = (int)(my - super.getHeight() / 2.0);

		mOffsetX -= dx;
		mOffsetY -= dy;

		if (scroll > 0 && mScale < 20)
		{
			mScale *= 1.2;
			mOffsetX *= 1.2;
			mOffsetY *= 1.2;
		}
		else if (scroll < 0 && mScale > 0.01)
		{
			mScale /= 1.2;
			mOffsetX /= 1.2;
			mOffsetY /= 1.2;
		}

		mOffsetX += dx;
		mOffsetY += dy;

		copyTo();
		repaint();
	}


	@Override
	public void mouseDragged(MouseEvent aEvent)
	{
		if (mImage != null)
		{
			mOffsetX = mStartX + (int)(aEvent.getX() - mOldOffsetX);
			mOffsetY = mStartY + (int)(aEvent.getY() - mOldOffsetY);

			copyTo();
			repaint();
		}
	}


	@Override
	public void mouseMoved(MouseEvent aEvent)
	{
	}


	private void copyTo()
	{
		if (mMirrorPanels != null)
		{
			for (_ImagePanel p : mMirrorPanels)
			{
				p.mStartX = mStartX;
				p.mStartY = mStartY;
				p.mOffsetX = mOffsetX;
				p.mOffsetY = mOffsetY;
				p.mOldOffsetX = mOldOffsetX;
				p.mOldOffsetY = mOldOffsetY;
				p.mScale = mScale;
				p.repaint();
			}
		}
	}
}
