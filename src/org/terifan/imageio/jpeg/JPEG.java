package org.terifan.imageio.jpeg;


public class JPEG
{
	public APP0Segment mAPP0Segment;
	public APP2Segment mAPP2Segment;
	public SOFSegment mSOFSegment;
	public SOSSegment mSOSSegment;
	public DQTSegment mDQTSegment;
	public DHTSegment mDHTSegment;
	public DACSegment mDACSegment;
	public APP14Segment mColorSpaceTransform;

	public int[][][][] mCoefficients;

	public ComponentInfo[] mComponentInfo;
	public int[] mMCUComponentIndices;
	public int mMCUBlockCount;
	public ColorSpace mColorSpace;

	public int mRestartInterval;
	public int mRestartMarkerIndex;
	public int mBlockCount;


	public JPEG()
	{
		mAPP0Segment = new APP0Segment();
		mDQTSegment = new DQTSegment();
	}


	public int[][][][] getCoefficients()
	{
		return mCoefficients;
	}


	public ColorSpace getColorSpace()
	{
		if (mColorSpace == null)
		{
			mColorSpace = guessColorSpace();
		}
		return mColorSpace;
	}


	public ColorSpace guessColorSpace()
	{
		if (mColorSpaceTransform != null)
		{
			switch (mColorSpaceTransform.getTransform())
			{
				case 1:
					return JPEGImageIO.createColorSpaceInstance("ycbcr");
				case 2:
					return JPEGImageIO.createColorSpaceInstance("ycck");
				default:
					return JPEGImageIO.createColorSpaceInstance(mSOFSegment.getComponents().length == 3 ? "rgb" : "cmyk");
			}
		}

		return JPEGImageIO.createColorSpaceInstance(mSOFSegment.getColorSpaceName());
	}
}
