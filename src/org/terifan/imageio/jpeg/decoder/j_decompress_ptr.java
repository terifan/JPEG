package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.ComponentInfo;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;


public class j_decompress_ptr
{
	public int[] arith_dc_L = new int[16];
	public int[] arith_dc_U = new int[16];
	public int[] arith_ac_K = new int[NUM_ARITH_TBLS];

	public int Ss;
	public int Se;
	public int Ah;
	public int Al;
	arith_entropy_ptr entropy;
	int[] MCU_membership;
	ComponentInfo[] cur_comp_info;
	int num_components;
	int blocks_in_MCU;
	int comps_in_scan;
	int[] natural_order;
	
	int restart_interval;
	int lim_Se;
	boolean progressive_mode;
	int unread_marker;
	int[][] coef_bits;


	public j_decompress_ptr()
	{
		for (int i = 0; i < NUM_ARITH_TBLS; i++) {
		  arith_dc_L[i] = 0;
		  arith_dc_U[i] = 1;
		  arith_ac_K[i] = 5;
		}
	}
}
