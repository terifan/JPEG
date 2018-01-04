package org.terifan.imageio.jpeg;

import java.awt.color.ICC_Profile;
import static org.terifan.imageio.jpeg.JPEGConstants.DCTSIZE2;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;


public class JPEG
{
	public int[] arith_dc_L = new int[NUM_ARITH_TBLS];
	public int[] arith_dc_U = new int[NUM_ARITH_TBLS];
	public int[] arith_ac_K = new int[NUM_ARITH_TBLS];

	public SOFSegment mSOFSegment;
	public boolean mArithmetic;
	public boolean mProgressive;
	public boolean mOptimizedHuffman;
	public int num_hor_mcu;
	public int num_ver_mcu;
	public int width;
	public int height;
	public int mDensitiesUnits;
	public int mDensityX;
	public int mDensityY;
	public int precision;
	public ColorSpace mColorSpace = ColorSpace.YCBCR;

	public QuantizationTable[] mQuantizationTables = new QuantizationTable[8];
	public HuffmanTable[][] mHuffmanTables = new HuffmanTable[4][2];

	public int[][][][] mCoefficients;

	public ComponentInfo[] components;
	public JPEGEntropyState entropy;
	public int[] MCU_membership;
	public ComponentInfo[] cur_comp_info;
	public int num_components;
	public int blocks_in_MCU;
	public int comps_in_scan;
	public boolean saw_Adobe_marker;
	public int Adobe_transform;

	public int Ss;
	public int Se;
	public int Ah;
	public int Al;
	public int lim_Se = DCTSIZE2 - 1;
	public int[][] coef_bits;

	public int restart_interval;
	public int restartMarkerIndex;
	public ICC_Profile mICCProfile;

	public HuffmanTable[] dc_huff_tbl_ptrs = new HuffmanTable[4];
	public HuffmanTable[] ac_huff_tbl_ptrs = new HuffmanTable[4];
	
//	public int next_output_byte_offset;
//	public byte[] next_output_byte = new byte[16];
//	public int free_in_buffer = 16;
	
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
}
