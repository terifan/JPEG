package org.terifan.imageio.jpeg;

import java.awt.color.ICC_Profile;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_HUFF_TBLS;


public class JPEG
{
	public SOFSegment mSOFSegment;

	public int[][][][] mCoefficients;

	public ComponentInfo[] mComponentInfo;
	public int[] mMCUComponentIndices;
	public int mMCUBlockCount;
	public int mScanBlockCount;

	public QuantizationTable[] mQuantizationTables;

	public HuffmanTable[][] mHuffmanTables;
	public HuffmanTable[] mHuffmanDCTables;
	public HuffmanTable[] mHuffmanACTables;

	public ColorSpace mColorSpace;
	public ICC_Profile mICCProfile;
	public APP14Segment mColorSpaceTransform;
	public APP0Segment mJFIFSegmentMarker;

	public int[] mArithDCL;
	public int[] mArithDCU;
	public int[] mArithACK;

	public int Ss;
	public int Se;
	public int Ah;
	public int Al;

	public int mDensitiesUnits;
	public int mDensityX;
	public int mDensityY;
	public int mRestartInterval;
	public int mRestartMarkerIndex;
	public int mBlockCount;


	public JPEG()
	{
		mDensitiesUnits = 1;
		mDensityX = 72;
		mDensityY = 72;

		mQuantizationTables = new QuantizationTable[8];

		mHuffmanTables = new HuffmanTable[NUM_HUFF_TBLS][2];
		mHuffmanDCTables = new HuffmanTable[NUM_HUFF_TBLS];
		mHuffmanACTables = new HuffmanTable[NUM_HUFF_TBLS];

		mArithACK = new int[NUM_ARITH_TBLS];
		mArithDCU = new int[NUM_ARITH_TBLS];
		mArithDCL = new int[NUM_ARITH_TBLS];

		for (int i = 0; i < NUM_ARITH_TBLS; i++)
		{
			mArithDCL[i] = 0;
			mArithDCU[i] = 1;
			mArithACK[i] = 5;
		}
	}


	public int[][][][] getCoefficients()
	{
		return mCoefficients;
	}


	public ColorSpace getColorSpace()
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
