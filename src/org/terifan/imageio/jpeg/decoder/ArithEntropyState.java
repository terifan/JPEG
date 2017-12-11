package org.terifan.imageio.jpeg.decoder;

import static org.terifan.imageio.jpeg.JPEGConstants.MAX_COMPS_IN_SCAN;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;


class ArithEntropyState 
{
  int c;  // C register, base of coding interval + input bit buffer 
  int a;  // A register, normalized size of coding interval 
  int ct; // bit shift counter, # of bits left in bit buffer part of C. init: ct = -16, run: ct = 0..7, error: ct = -1
  int[] last_dc_val = new int[MAX_COMPS_IN_SCAN]; // last DC coef for each component
  int[] dc_context = new int[MAX_COMPS_IN_SCAN];  // context index for DC conditioning

  int restarts_to_go;	// MCUs left in this restart interval

  // Pointers to statistics areas (these workspaces have image lifespan)
  int[] dc_stats = new int[NUM_ARITH_TBLS];
  int[] ac_stats = new int[NUM_ARITH_TBLS];

  // Statistics bin for coding with fixed probability 0.5
  int[] fixed_bin = new int[4];
}
