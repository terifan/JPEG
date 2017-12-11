package org.terifan.imageio.jpeg;

import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.decoder.arith_entropy_ptr;
import org.terifan.imageio.jpeg.decoder.arith_entropy_ptr;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;


public class JPEG
{
	public int[] arith_dc_L = new int[16];
	public int[] arith_dc_U = new int[16];
	public int[] arith_ac_K = new int[NUM_ARITH_TBLS];

	public int Ss;
	public int Se;
	public int Ah;
	public int Al;
	public arith_entropy_ptr entropy;
	public int[] MCU_membership;
	public ComponentInfo[] cur_comp_info;
	public int num_components;
	public int blocks_in_MCU;
	public int comps_in_scan;

	public int restart_interval;
	public int lim_Se;
	public boolean progressive_mode;
	public int unread_marker;
	public int[][] coef_bits;


	public JPEG()
	{
		for (int i = 0; i < NUM_ARITH_TBLS; i++) {
		  arith_dc_L[i] = 0;
		  arith_dc_U[i] = 1;
		  arith_ac_K[i] = 5;
		}
	}
}
