package org.terifan.imageio.jpeg;

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
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.decoder.IDCT;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.JPEGImageReaderImpl;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;
import org.terifan.imageio.jpeg.encoder.FDCT;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriterImpl;
import org.terifan.imageio.jpeg.encoder.ProgressionScript;
import org.terifan.imageio.jpeg.encoder.QuantizationTableFactory;


public class JPEGImageIO
{
	private Class<? extends IDCT> mIDCT;
	private Class<? extends FDCT> mFDCT;
	private ProgressionScript mProgressionScript;
	private boolean mArithmetic;
	private boolean mOptimizedHuffman;
	private boolean mProgressive;
	private int mQuality;
	private boolean mUpdateProgressiveImage;


	public JPEGImageIO()
	{
		mQuality = 90;
		mIDCT = IDCTIntegerFast.class;
		mFDCT = FDCTIntegerFast.class;
	}


	public BufferedImage read(Object aInput) throws JPEGImageIOException
	{
		JPEG jpeg = new JPEG();
		IDCT idct = createIDCTInstance();
		JPEGImage image;

		try (BitInputStream in = new BitInputStream(toInputStream(aInput)))
		{
			JPEGImageReaderImpl reader = new JPEGImageReaderImpl();
			image = reader.read(in, jpeg, idct, true, mUpdateProgressiveImage);
		}
		catch (IOException e)
		{
			throw new JPEGImageIOException(e);
		}

		if (image == null)
		{
			return null;
		}

		return new ColorTransformer().colorTransform(jpeg, image);
	}


	public void write(BufferedImage aInput, Object aOutput) throws JPEGImageIOException
	{
		FDCT fdct = createFDCTInstance();

		// chroma subsampling 4:4:4 4:2:2 4:2:0
		ComponentInfo lu = new ComponentInfo(ComponentInfo.Y, 1, 0, 2, 2);
		ComponentInfo cb = new ComponentInfo(ComponentInfo.CB, 2, 1, 1, 1);
		ComponentInfo cr = new ComponentInfo(ComponentInfo.CR, 3, 1, 1, 1);
		ComponentInfo[] components = new ComponentInfo[]{lu, cb, cr};

		JPEG jpeg = new JPEG();
		jpeg.mArithmetic = mArithmetic;
		jpeg.mProgressive = mProgressive;
		jpeg.mOptimizedHuffman = mOptimizedHuffman;
		jpeg.mSOFSegment = new SOFSegment(jpeg, aInput.getWidth(), aInput.getHeight(), 8, components);
		jpeg.mQuantizationTables = new QuantizationTable[2];
		jpeg.mQuantizationTables[0] = QuantizationTableFactory.buildQuantTable(mQuality, 0);
		jpeg.mQuantizationTables[1] = QuantizationTableFactory.buildQuantTable(mQuality, 1);

		ImageSampler.sampleImage(jpeg, aInput, fdct);

		encode(jpeg, aOutput);
	}


	public void transcode(Object aInput, Object aOutput) throws JPEGImageIOException
	{
		JPEG jpeg = decode(aInput);

		if (jpeg.mSOFSegment.getComponents() == null)
		{
			throw new IllegalStateException("Error decoding source image");
		}

		jpeg.mArithmetic = mArithmetic;
		jpeg.mProgressive = mProgressive;
		jpeg.mOptimizedHuffman = mOptimizedHuffman;
		jpeg.mRestartInterval = 0;

		encode(jpeg, aOutput);
	}


	public JPEG decode(Object aInput) throws JPEGImageIOException
	{
		JPEG jpeg = new JPEG();

		try (BitInputStream in = new BitInputStream(toInputStream(aInput)))
		{
			JPEGImageReaderImpl reader = new JPEGImageReaderImpl();
			reader.read(in, jpeg, null, false, false);
		}
		catch (IOException e)
		{
			throw new JPEGImageIOException(e);
		}

		return jpeg;
	}


	public void encode(JPEG aJpeg, Object aOutput) throws JPEGImageIOException
	{
		try (final BitOutputStream out = new BitOutputStream(toOutputStream(aOutput)))
		{
			JPEGImageWriterImpl writer = new JPEGImageWriterImpl();
			writer.create(aJpeg, out);
			writer.encodeCoefficients(aJpeg, out, mProgressionScript);
			writer.finish(aJpeg, out);
		}catch (IOException e)
		{
			throw new JPEGImageIOException(e);
		}
	}


	protected FDCT createFDCTInstance() throws JPEGImageIOException
	{
		try
		{
			return mFDCT.newInstance();
		}
		catch (IllegalAccessException | InstantiationException e)
		{
			throw new JPEGImageIOException(e);
		}
	}


	protected IDCT createIDCTInstance()
	{
		try
		{
			return mIDCT.newInstance();
		}
		catch (IllegalAccessException | InstantiationException e)
		{
			throw new JPEGImageIOException(e);
		}
	}


	protected InputStream toInputStream(Object aInput) throws IOException
	{
		if (aInput instanceof File) return new BufferedInputStream(new FileInputStream((File)aInput));
		if (aInput instanceof String) return new BufferedInputStream(new FileInputStream((String)aInput));
		if (aInput instanceof byte[]) return new ByteArrayInputStream((byte[])aInput);
		if (aInput instanceof URL) return new BufferedInputStream(((URL)aInput).openStream());

		return (InputStream)aInput;
	}


	protected OutputStream toOutputStream(Object aOutput) throws IOException
	{
		if (aOutput instanceof File) return new BufferedOutputStream(new FileOutputStream((File)aOutput));
		if (aOutput instanceof String) return new BufferedOutputStream(new FileOutputStream((String)aOutput));
		return (OutputStream)aOutput;
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
