package org.terifan.imageio.jpeg;

import static org.terifan.imageio.jpeg.JPEGConstants.DCTSIZE2;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;
import org.terifan.imageio.jpeg.decoder.ArithEntropyState;


public class JPEG
{
	public int[] arith_dc_L = new int[NUM_ARITH_TBLS];
	public int[] arith_dc_U = new int[NUM_ARITH_TBLS];
	public int[] arith_ac_K = new int[NUM_ARITH_TBLS];

	public int Ss;
	public int Se;
	public int Ah;
	public int Al;
	public ArithEntropyState entropy;
	public int[] MCU_membership;
	public ComponentInfo[] cur_comp_info;
	public int num_components;
	public int blocks_in_MCU;
	public int comps_in_scan;
	public int lim_Se = DCTSIZE2 - 1;

	public int restart_interval;
	public int unread_marker;
	public int[][] coef_bits;

	public int mDensitiesUnits;
	public int mDensityX;
	public int mDensityY;

	public QuantizationTable[] mQuantizationTables = new QuantizationTable[8];

	public boolean mArithmetic;
	public boolean mProgressive;
	public int num_hor_mcu;
	public int num_ver_mcu;


	public JPEG()
	{
		for (int i = 0; i < NUM_ARITH_TBLS; i++) {
		  arith_dc_L[i] = 0;
		  arith_dc_U[i] = 1;
		  arith_ac_K[i] = 5;
		}
	}
}
