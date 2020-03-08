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
import java.io.PrintStream;
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
	private CompressionType mCompressionType;
	private double mQuality;
	private boolean mUpdateProgressiveImage;
	private SubsamplingMode mSubsampling;
	private Log mLog;


	public JPEGImageIO()
	{
		mQuality = 90;
		mIDCT = IDCTIntegerFast.class;
		mFDCT = FDCTIntegerFast.class;
		mSubsampling = SubsamplingMode._422;
		mCompressionType = CompressionType.Huffman;
		mLog = new Log();
	}


	public BufferedImage read(Object aInput) throws JPEGImageIOException
	{
		JPEG jpeg = new JPEG();
		jpeg.mColorSpace = ColorSpace.YCBCR;

		IDCT idct = createIDCTInstance();
		BufferedImage image;

		try (BitInputStream in = new BitInputStream(toInputStream(aInput)))
		{
			JPEGImageReaderImpl reader = new JPEGImageReaderImpl();
			image = reader.decode(in, jpeg, mLog, idct, true, mUpdateProgressiveImage);
		}
		catch (IOException e)
		{
			throw new JPEGImageIOException(e);
		}

		if (image != null)
		{
			ColorSpaceTransform.transform(jpeg, image);
		}

		return image;
	}


	public void write(BufferedImage aInput, Object aOutput) throws JPEGImageIOException
	{
		FDCT fdct = createFDCTInstance();

		int[][] samplingFactors = mSubsampling.getSamplingFactors();

		ComponentInfo lu = new ComponentInfo(ComponentInfo.Y, 1, 0, samplingFactors[0][0], samplingFactors[0][1]);
		ComponentInfo cb = new ComponentInfo(ComponentInfo.CB, 2, 1, samplingFactors[1][0], samplingFactors[1][1]);
		ComponentInfo cr = new ComponentInfo(ComponentInfo.CR, 3, 1, samplingFactors[2][0], samplingFactors[2][1]);
		ComponentInfo[] components = new ComponentInfo[]{lu, cb, cr};

		JPEG jpeg = new JPEG();
		jpeg.mColorSpace = ColorSpace.YCBCR;
		jpeg.mSOFSegment = new SOFSegment(jpeg, mCompressionType, aInput.getWidth(), aInput.getHeight(), 8, components);
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

		jpeg.mSOFSegment.setCompressionType(mCompressionType);
		jpeg.mRestartInterval = 0;

		encode(jpeg, aOutput);
	}


	public JPEG decode(Object aInput) throws JPEGImageIOException
	{
		JPEG jpeg = new JPEG();

		try (BitInputStream in = new BitInputStream(toInputStream(aInput)))
		{
			JPEGImageReaderImpl reader = new JPEGImageReaderImpl();
			reader.decode(in, jpeg, mLog, null, false, false);
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
			writer.create(aJpeg, out, mLog);
			writer.encode(aJpeg, out, mLog, aJpeg.mSOFSegment.getCompressionType(), mProgressionScript);
			writer.finish(aJpeg, out, mLog);
		}
		catch (IOException e)
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


	public CompressionType getCompressionType()
	{
		return mCompressionType;
	}


	public JPEGImageIO setCompressionType(CompressionType aCompressionType)
	{
		mCompressionType = aCompressionType;
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


	public double getQuality()
	{
		return mQuality;
	}


	public JPEGImageIO setQuality(double aQuality)
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


	public SubsamplingMode getSubsampling()
	{
		return mSubsampling;
	}


	public JPEGImageIO setSubsampling(SubsamplingMode aSubsampling)
	{
		mSubsampling = aSubsampling;
		return this;
	}


	public Log getLog()
	{
		return mLog;
	}


	public JPEGImageIO setLog(Log aLog)
	{
		mLog = aLog;
		return this;
	}


	public JPEGImageIO setLog(PrintStream aPrintStream)
	{
		mLog = new Log().setPrintStream(aPrintStream);
		return this;
	}
}
