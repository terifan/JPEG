package org.terifan.imageio.jpeg.decoder;

import static org.terifan.imageio.jpeg.JPEGConstants.MAX_COMPS_IN_SCAN;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;
import static org.terifan.imageio.jpeg.JPEGConstants.DC_STAT_BINS;


public class ArithEntropyState
{
	public int c;  // C register, base of coding interval + input bit buffer
	public int a;  // A register, normalized size of coding interval
	public int ct; // bit shift counter, # of bits left in bit buffer part of C. init: ct = -16, run: ct = 0..7, error: ct = -1
	public int[] last_dc_val = new int[MAX_COMPS_IN_SCAN]; // last DC coef for each component
	public int[] dc_context = new int[DC_STAT_BINS];  // context index for DC conditioning

	public int restarts_to_go;	// MCUs left in this restart interval

	// Pointers to statistics areas (these workspaces have image lifespan)
	public int[][] dc_stats = new int[NUM_ARITH_TBLS][];
	public int[][] ac_stats = new int[NUM_ARITH_TBLS][];

	// Statistics bin for coding with fixed probability 0.5
	public int[] fixed_bin = new int[4];
	public int decode_mcu;



	// encode -------

	public int sc;        /* counter for stacked 0xFF values which might overflow */
	public int zc;          /* counter for pending 0x00 output values which might *
							* be discarded at the end ("Pacman" termination) */
	public int buffer;                /* buffer for most recent output byte != 0xFF */
	public int next_restart_num;

	public int encode_mcu;
}
