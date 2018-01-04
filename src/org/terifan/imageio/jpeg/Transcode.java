package org.terifan.imageio.jpeg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriter;


public class Transcode
{
	private boolean mArithmetic;
	private boolean mOptimizedHuffman;
	private boolean mProgressive;


	public void transcode(File aFile, OutputStream aOutputStream) throws IOException
	{
		try (InputStream in = new BufferedInputStream(new FileInputStream(aFile)))
		{
			transcode(in, aOutputStream);
		}
	}


	public void transcode(URL aInputStream, OutputStream aOutputStream) throws IOException
	{
		try (InputStream in = new BufferedInputStream(aInputStream.openStream()))
		{
			transcode(in, aOutputStream);
		}
	}


	public void transcode(InputStream aInputStream, OutputStream aOutputStream) throws IOException
	{
		JPEG jpeg = new JPEGImageReader(aInputStream).decode();

		if (jpeg.components == null)
		{
			throw new IllegalStateException("Error decoding source image");
		}

		jpeg.mArithmetic = mArithmetic;
		jpeg.mProgressive = mProgressive;
		jpeg.mOptimizedHuffman = mOptimizedHuffman;
		jpeg.restart_interval = 0;

		JPEGImageWriter writer = new JPEGImageWriter(aOutputStream);
		writer.create(jpeg);
		writer.encodeCoefficients(jpeg);
		writer.finish(jpeg);
	}


	public boolean isArithmetic()
	{
		return mArithmetic;
	}


	public Transcode setArithmetic(boolean aArithmetic)
	{
		mArithmetic = aArithmetic;
		return this;
	}


	public boolean isOptimizedHuffman()
	{
		return mOptimizedHuffman;
	}


	public Transcode setOptimizedHuffman(boolean aOptimizedHuffman)
	{
		mOptimizedHuffman = aOptimizedHuffman;
		return this;
	}


	public boolean isProgressive()
	{
		return mProgressive;
	}


	public Transcode setProgressive(boolean aProgressive)
	{
		mProgressive = aProgressive;
		return this;
	}
}
