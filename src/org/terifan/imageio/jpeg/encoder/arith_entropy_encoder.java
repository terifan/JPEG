package org.terifan.imageio.jpeg.encoder;

import static org.terifan.imageio.jpeg.JPEGConstants.MAX_COMPS_IN_SCAN;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;


class arith_entropy_encoder
{
	int encode_mcu;

	int c; /* C register, base of coding interval, layout as in sec. D.1.3 */
	int a;               /* A register, normalized size of coding interval */
	int sc;        /* counter for stacked 0xFF values which might overflow */
	int zc;          /* counter for pending 0x00 output values which might *
							* be discarded at the end ("Pacman" termination) */
	int ct;  /* bit shift counter, determines when next byte will be written */
	int buffer;                /* buffer for most recent output byte != 0xFF */

	int[] last_dc_val = new int[MAX_COMPS_IN_SCAN]; /* last DC coef for each component */
	int[] dc_context = new int[MAX_COMPS_IN_SCAN]; /* context index for DC conditioning */

	int restarts_to_go;	/* MCUs left in this restart interval */
	int next_restart_num;		/* next restart number to write (0-7) */

	/* Pointers to statistics areas (these workspaces have image lifespan) */
	int[][] dc_stats = new int[NUM_ARITH_TBLS][];
	int[][] ac_stats = new int[NUM_ARITH_TBLS][];

	/* Statistics bin for coding with fixed probability 0.5 */
	int[] fixed_bin = new int[4];
}
