package org.terifan.imageio.jpeg;


public enum CompressionType
{
	Huffman(SegmentMarker.SOF0),
	HuffmanOptimized(SegmentMarker.SOF0),
	HuffmanProgressive(SegmentMarker.SOF2),
	Arithmetic(SegmentMarker.SOF9),
	ArithmeticProgressive(SegmentMarker.SOF10);


	private final SegmentMarker mSegmentMarker;


	private CompressionType(SegmentMarker aSegmentMarker)
	{
		mSegmentMarker = aSegmentMarker;
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
}
