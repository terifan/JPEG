package org.terifan.imageio.jpeg;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.decoder.IDCT;


public class JPEGImage
{
	protected BufferedImage mImage;
	protected int[] mIntBuffer;
	protected byte[] mByteBuffer;
	protected FixedThreadExecutor mExecutorService;


	void configure(int aWidth, int aHeight, int aType)
	{
		mImage = new BufferedImage(aWidth, aHeight, aType);

		if (aType == BufferedImage.TYPE_INT_RGB)
		{
			DataBufferInt buffer = (DataBufferInt)mImage.getRaster().getDataBuffer();
			mIntBuffer = buffer.getData();
		}
		else
		{
			DataBufferByte buffer = (DataBufferByte)mImage.getRaster().getDataBuffer();
			mByteBuffer = buffer.getData();
		}

		mExecutorService = new FixedThreadExecutor(1f);
	}


	void finish()
	{
		if (mExecutorService != null)
		{
			mExecutorService.shutdown();
		}
	}


	public void endOfScan(int aProgressionLevel)
	{
//		mExecutorService.submit(()->{
//			try
//			{
//				ImageIO.write(mImage, "png", new File("d:\\dev\\out" + aProgressionLevel + ".png"));
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace(System.out);
//			}
//		});

		mExecutorService.shutdown();

		try
		{
			ImageIO.write(mImage, "png", new File("d:\\dev\\out\\" + aProgressionLevel + ".png"));
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}

		mExecutorService = new FixedThreadExecutor(1f);
	}


	public void update(JPEG aJPEG, IDCT aIDCT, int aMCUY, int[][][] aCoefficients)
	{
		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();

		int[][][] workBlock = new int[numHorMCU][aJPEG.mMCUBlockCount][64];

		for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
		{
			for (int blockIndex = 0; blockIndex < workBlock[0].length; blockIndex++)
			{
				System.arraycopy(aCoefficients[mcuX][blockIndex], 0, workBlock[mcuX][blockIndex], 0, 64);
			}
		}

		mExecutorService.submit(()->
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
				ImageTransdecode.transform(aJPEG, aIDCT, this, mcuX, aMCUY, workBlock[mcuX]);
			}
		});
	}


	public BufferedImage getBufferedImage()
	{
		return mImage;
	}


	public int getWidth()
	{
		return mImage.getWidth();
	}


	public int getHeight()
	{
		return mImage.getHeight();
	}


	public int getRGB(int aX, int aY)
	{
		return mImage.getRGB(aX, aY);
	}


	public void setRGB(int aX, int aY, int aRGB)
	{
		mImage.setRGB(aX, aY, aRGB);
	}


	public void setRGB(int aX, int aY, int aW, int aH, int[] aRGB)
	{
		if (mIntBuffer != null)
		{
			for (int y = aY, h = Math.min(aY + aH, mImage.getHeight()), src = 0, dst = aY * mImage.getWidth() + aX, w = Math.min(aW, mImage.getWidth() - aX); y < h; y++, src+=aW, dst+=mImage.getWidth())
			{
				System.arraycopy(aRGB, src, mIntBuffer, dst, w);
			}
		}
		else
		{
			for (int y = aY, h = Math.min(aY + aH, mImage.getHeight()), src = 0, dst = aY * mImage.getWidth() + aX, w = Math.min(aW, mImage.getWidth() - aX); y < h; y++, src+=aW, dst+=mImage.getWidth())
			{
				for (int i = 0; i < w; i++)
				{
					mByteBuffer[dst + i] = (byte)aRGB[src + i];
				}
			}
		}
	}
}
