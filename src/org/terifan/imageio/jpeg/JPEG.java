package org.terifan.imageio.jpeg;

import java.awt.color.ICC_Profile;
import static org.terifan.imageio.jpeg.JPEGConstants.DCTSIZE2;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;


public class JPEG
{
	public SOFSegment mSOFSegment;
	public boolean mArithmetic;
	public boolean mProgressive;
	public boolean mOptimizedHuffman;

	public QuantizationTable[] mQuantizationTables = new QuantizationTable[8];

	public HuffmanTable[][] mHuffmanTables = new HuffmanTable[4][2];
	public HuffmanTable[] dc_huff_tbl_ptrs = new HuffmanTable[4];
	public HuffmanTable[] ac_huff_tbl_ptrs = new HuffmanTable[4];

	public ColorSpace mColorSpace = ColorSpace.YCBCR;
	public ICC_Profile mICCProfile;
	public boolean mHasAdobeMarker;

	public int[][][][] mCoefficients;

	public int mDensitiesUnits;
	public int mDensityX;
	public int mDensityY;

	public int mRestartInterval;
	public int mRestartMarkerIndex;

	public int[] arith_dc_L = new int[NUM_ARITH_TBLS];
	public int[] arith_dc_U = new int[NUM_ARITH_TBLS];
	public int[] arith_ac_K = new int[NUM_ARITH_TBLS];

	public int num_hor_mcu;
	public int num_ver_mcu;

	public JPEGEntropyState entropy;
	public int[] MCU_membership;
	public ComponentInfo[] cur_comp_info;
	public int blocks_in_MCU;
	public int comps_in_scan;
	public int Ss;
	public int Se;
	public int Ah;
	public int Al;
	public int lim_Se = DCTSIZE2 - 1;
	public int[][] coef_bits;


	public JPEG()
	{
		for (int i = 0; i < NUM_ARITH_TBLS; i++)
		{
			arith_dc_L[i] = 0;
			arith_dc_U[i] = 1;
			arith_ac_K[i] = 5;
		}
	}


	public int[][][][] getCoefficients()
	{
		return mCoefficients;
	}


	public String getSubSampling()
	{
		StringBuilder sb = new StringBuilder();
		for (ComponentInfo ci : mSOFSegment.getComponents())
		{
			if (sb.length() > 0)
			{
				sb.append(",");
			}
			sb.append(ci.getHorSampleFactor() + "x" + ci.getVerSampleFactor());
		}

		switch (sb.toString())
		{
			case "1x1,1x1,1x1":	return "4:4:4"; // 1 1
			case "1x2,1x1,1x1":	return "4:4:0"; // 1 2
			case "2----------":	return "4:4:1"; // 1 4
			case "2x1,1x1,1x1":	return "4:2:2"; // 2 1
			case "2x2,1x1,1x1":	return "4:2:0"; // 2 2
			case "3----------":	return "4:2:1"; // 2 4
			case "4x1,1x1,1x1":	return "4:1:1"; // 4 1
			case "4x1,2x1,2x1":	return "4:1:0"; // 4 2
		}

		return sb.toString();
	}
}
