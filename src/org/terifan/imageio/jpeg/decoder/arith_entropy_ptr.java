package org.terifan.imageio.jpeg.decoder;

import static org.terifan.imageio.jpeg.JPEGConstants.MAX_COMPS_IN_SCAN;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;


public class arith_entropy_ptr
{
	public int[][] dc_stats = new int[NUM_ARITH_TBLS][];
	public int[][] ac_stats = new int[NUM_ARITH_TBLS][];
	public int[] fixed_bin = new int[4];
	public int[] dc_context = new int[MAX_COMPS_IN_SCAN];
	public int[] last_dc_val = new int[MAX_COMPS_IN_SCAN];

	int decode_mcu;
	int a;
	int c;
	int ct;
	int restarts_to_go;
}
