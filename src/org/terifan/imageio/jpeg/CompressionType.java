package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.ArithmeticDecoder;
import org.terifan.imageio.jpeg.decoder.Decoder;
import org.terifan.imageio.jpeg.decoder.HuffmanDecoder;


public enum CompressionType
{
	Huffman(SegmentMarker.SOF0, HuffmanDecoder.class),
	HuffmanOptimized(SegmentMarker.SOF0, HuffmanDecoder.class),
	HuffmanProgressive(SegmentMarker.SOF2, HuffmanDecoder.class),
	Arithmetic(SegmentMarker.SOF9, ArithmeticDecoder.class),
	ArithmeticProgressive(SegmentMarker.SOF10, ArithmeticDecoder.class);


	private final SegmentMarker mSegmentMarker;
	private final Class<? extends Decoder> mDecoderType;


	private CompressionType(SegmentMarker aSegmentMarker, Class<? extends Decoder> aDecoderType)
	{
		mSegmentMarker = aSegmentMarker;
		mDecoderType = aDecoderType;
	}


	public static CompressionType decode(SegmentMarker aMarker) throws IOException
	{
		switch (aMarker)
		{
			case SOF0:
				return CompressionType.Huffman;
			case SOF2:
				return CompressionType.HuffmanProgressive;
			case SOF9:
				return CompressionType.Arithmetic;
			case SOF10:
				return CompressionType.ArithmeticProgressive;
			default:
				throw new IOException("Image encoding not supported");
		}
	}


	SegmentMarker getSegmentMarker()
	{
		return mSegmentMarker;
	}


	public boolean isProgressive()
	{
		return this == HuffmanProgressive || this == ArithmeticProgressive;
	}


	public boolean isArithmetic()
	{
		return this == Arithmetic || this == ArithmeticProgressive;
	}


	public Decoder createDecoderInstance()
	{
		try
		{
			return mDecoderType.newInstance();
		}
		catch (IllegalAccessException | InstantiationException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
