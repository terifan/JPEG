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
import java.util.function.Function;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.decoder.IDCT;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.decoder.JPEGImageReaderImpl;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;
import org.terifan.imageio.jpeg.encoder.FDCT;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriterImpl;
import org.terifan.imageio.jpeg.encoder.ProgressionScript;
import org.terifan.imageio.jpeg.encoder.QuantizationTableFactory;


public class JPEGImageIO
{
	private static Function<String, ColorSpace> mColorSpaceFactory = name ->
	{
		switch (name)
		{
			case "grayscale":
				return new ColorSpaceRGBGrayscale();
			case "ycbcr":
				return new ColorSpaceRGBYCbCrTab();
//				return new ColorSpaceRGBYCbCrFloat();
//				return new ColorSpaceRGBYCbCrFP();
			case "rgb":
				return new ColorSpaceRGBRGB();
			default:
				throw new IllegalArgumentException("Unsupported color space: " + name);
		}
	};

	private Class<? extends IDCT> mIDCT;
	private Class<? extends FDCT> mFDCT;
	private ProgressionScript mProgressionScript;
	private CompressionType mCompressionType;
	private double mQuality;
	private boolean mUpdateProgressiveImage;
	private SubsamplingMode mSubsampling;
	private Log mLog;
	private Runnable mRenderListener;


	public JPEGImageIO()
	{
		mQuality = 90;
		mIDCT = IDCTIntegerSlow.class;
		mFDCT = FDCTIntegerSlow.class;
		mSubsampling = SubsamplingMode._422;
		mCompressionType = CompressionType.Huffman;
		mProgressionScript = ProgressionScript.DEFAULT;
		mLog = new Log();
	}


	public BufferedImage read(Object aInput) throws JPEGImageIOException
	{
		try (BitInputStream in = new BitInputStream(toInputStream(aInput)))
		{
			JPEG jpeg = new JPEG();

			IDCT idct = createIDCTInstance();

			JPEGImageReaderImpl reader = new JPEGImageReaderImpl();

			JPEGImage image = new JPEGImage();
			boolean success;

			try (FixedThreadExecutor threadPool = new FixedThreadExecutor(1f))
			{
				success = reader.decode(in, jpeg, mLog, idct, image, true, mUpdateProgressiveImage, false, threadPool);
			}

			ColorICCTransform.transform(jpeg, image);

			return image.getBufferedImage();
		}
		catch (IOException e)
		{
			throw new JPEGImageIOException(e);
		}
	}


	public void write(BufferedImage aInput, Object aOutput) throws JPEGImageIOException
	{
		FDCT fdct = createFDCTInstance();

		JPEG jpeg = new JPEG();
		jpeg.mColorSpace = JPEGImageIO.createColorSpaceInstance("ycbcr");
		jpeg.mSOFSegment = new SOFSegment(jpeg, mCompressionType, aInput.getWidth(), aInput.getHeight(), 8, getComponentsFromImage(aInput));
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
			try (FixedThreadExecutor threadPool = new FixedThreadExecutor(1f))
			{
				reader.decode(in, jpeg, mLog, null, null, false, false, true, threadPool);
			}
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


	protected ComponentInfo[] getComponentsFromImage(BufferedImage aInput) throws IllegalArgumentException
	{
		int[][] samplingFactors = mSubsampling.getSamplingFactors();

		switch (aInput.getType())
		{
			case BufferedImage.TYPE_BYTE_GRAY:
			case BufferedImage.TYPE_BYTE_BINARY:
			case BufferedImage.TYPE_USHORT_GRAY:
				return new ComponentInfo[]
				{
					new ComponentInfo(ComponentInfo.Type.Y.ordinal(), 1, 0, samplingFactors[0][0], samplingFactors[0][1])
				};
			default:
				return new ComponentInfo[]
				{
					new ComponentInfo(ComponentInfo.Type.Y.ordinal(), 1, 0, samplingFactors[0][0], samplingFactors[0][1]),
					new ComponentInfo(ComponentInfo.Type.CB.ordinal(), 2, 1, samplingFactors[1][0], samplingFactors[1][1]),
					new ComponentInfo(ComponentInfo.Type.CR.ordinal(), 3, 1, samplingFactors[2][0], samplingFactors[2][1])
				};
		}
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


	public static void setColorSpaceFactory(Function<String,ColorSpace> aFactory)
	{
		mColorSpaceFactory = aFactory;
	}


	public static ColorSpace createColorSpaceInstance(String aComponents)
	{
		return mColorSpaceFactory.apply(aComponents);
	}


	public Runnable getRenderListener()
	{
		return mRenderListener;
	}


	public JPEGImageIO setRenderListener(Runnable aRenderListener)
	{
		mRenderListener = aRenderListener;
		return this;
	}
}
