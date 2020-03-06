package org.terifan.imageio.jpeg.encoder;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.decoder.IDCT;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class JPEGImageIO
{
	private ProgressionScript mProgressionScript;
	private boolean mArithmetic;
	private boolean mOptimizedHuffman;
	private boolean mProgressive;
	private int mQuality;
	private Class<? extends IDCT> mIDCT;
	private Class<? extends FDCT> mFDCT;


	public JPEGImageIO()
	{
		mQuality = 90;
		mIDCT = IDCTIntegerFast.class;
		mFDCT = FDCTIntegerFast.class;
	}


	public BufferedImage read(Object aInput) throws JPEGImageIOException
	{
		try (BitInputStream in = new BitInputStream(toInputStream(aInput)))
		{
			return new JPEGImageReader().read(in, true);
		}
		catch (IOException e)
		{
			throw new JPEGImageIOException(e);
		}
	}


	public JPEG decode(Object aInput) throws JPEGImageIOException
	{
		try (BitInputStream in = new BitInputStream(toInputStream(aInput)))
		{
			JPEGImageReader reader = new JPEGImageReader();
			reader.read(in, false);
			return reader.getJPEG();
		}
		catch (IOException e)
		{
			throw new JPEGImageIOException(e);
		}
	}


	public void write(BufferedImage aInput, Object aOutput) throws JPEGImageIOException
	{
		try (BitOutputStream out = new BitOutputStream(toOutputStream(aOutput)))
		{
			JPEG jpeg = new JPEG();
			jpeg.mArithmetic = mArithmetic;
			jpeg.mProgressive = mProgressive;
			jpeg.mOptimizedHuffman = mOptimizedHuffman;

			FDCT fdct;

			try
			{
				fdct = mFDCT.newInstance();
			}
			catch (IllegalAccessException | InstantiationException e)
			{
				throw new JPEGImageIOException(e);
			}

			new JPEGImageWriter().write(jpeg, aInput, out, mQuality, mProgressionScript, fdct);
		}
		catch (IOException e)
		{
			throw new JPEGImageIOException(e);
		}
	}


	public void transcode(Object aInput, Object aOutput) throws IOException
	{
		try (BitInputStream in = new BitInputStream(toInputStream(aInput)); OutputStream out = toOutputStream(aOutput))
		{
			transcodeImpl(in, out);
		}
	}


	private void transcodeImpl(BitInputStream aInputStream, OutputStream aOutputStream) throws IOException
	{
		JPEGImageReader reader = new JPEGImageReader();
		reader.read(aInputStream, false);
		JPEG jpeg = reader.getJPEG();

		if (jpeg.components == null)
		{
			throw new IllegalStateException("Error decoding source image");
		}

		jpeg.mArithmetic = mArithmetic;
		jpeg.mProgressive = mProgressive;
		jpeg.mOptimizedHuffman = mOptimizedHuffman;
		jpeg.restart_interval = 0;

		JPEGImageWriter writer = new JPEGImageWriter();
		writer.setBitStream(new BitOutputStream(aOutputStream));
		writer.create(jpeg);
		writer.encodeCoefficients(jpeg);
		writer.finish(jpeg);
	}


	public boolean isArithmetic()
	{
		return mArithmetic;
	}


	public JPEGImageIO setArithmetic(boolean aArithmetic)
	{
		mArithmetic = aArithmetic;
		return this;
	}


	public boolean isOptimizedHuffman()
	{
		return mOptimizedHuffman;
	}


	public JPEGImageIO setOptimizedHuffman(boolean aOptimizedHuffman)
	{
		mOptimizedHuffman = aOptimizedHuffman;
		return this;
	}


	public boolean isProgressive()
	{
		return mProgressive;
	}


	public JPEGImageIO setProgressive(boolean aProgressive)
	{
		mProgressive = aProgressive;
		return this;
	}


	public ProgressionScript getProgressionScript()
	{
		return mProgressionScript;
	}


	public JPEGImageIO setProgressionScript(ProgressionScript aProgressionScript)
	{
		mProgressionScript = aProgressionScript;
		return this;
	}


	public int getQuality()
	{
		return mQuality;
	}


	public JPEGImageIO setQuality(int aQuality)
	{
		mQuality = aQuality;
		return this;
	}


	private InputStream toInputStream(Object aInput) throws IOException
	{
		if (aInput instanceof File) return new BufferedInputStream(new FileInputStream((File)aInput));
		if (aInput instanceof String) return new BufferedInputStream(new FileInputStream((String)aInput));
		if (aInput instanceof byte[]) return new ByteArrayInputStream((byte[])aInput);
		if (aInput instanceof URL) return new BufferedInputStream(((URL)aInput).openStream());

		return (InputStream)aInput;
	}


	private OutputStream toOutputStream(Object aOutput) throws IOException
	{
		if (aOutput instanceof File) return new BufferedOutputStream(new FileOutputStream((File)aOutput));
		if (aOutput instanceof String) return new BufferedOutputStream(new FileOutputStream((String)aOutput));
		return (OutputStream)aOutput;
	}


	public Class<? extends IDCT> getIDCT()
	{
		return mIDCT;
	}


	public JPEGImageIO setIDCT(Class<? extends IDCT> aClass)
	{
		mIDCT = aClass;
		return this;
	}


	public Class<? extends FDCT> getFDCT()
	{
		return mFDCT;
	}


	public JPEGImageIO setFDCT(Class<? extends FDCT> aClass)
	{
		mFDCT = aClass;
		return this;
	}
}
