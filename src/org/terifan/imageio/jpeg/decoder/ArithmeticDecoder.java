package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/*
 * jdarith.c
 *
 * Developed 1997-2015 by Guido Vollbeding.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains portable arithmetic entropy decoding routines for JPEG
 * (implementing the ISO/IEC IS 10918-1 and CCITT Recommendation ITU-T T.81).
 *
 * Both sequential and progressive modes are supported in this single module.
 *
 * Suspension is not currently supported in this module.
 */
public class ArithmeticDecoder
{
	private InputStream mInputStream;

	int NUM_ARITH_TBLS = 16;	/* Arith-coding tables are numbered 0..15 */
	int MAX_COMPS_IN_SCAN = 4;	/* JPEG limit on # of components in one scan */
	int MAX_SAMP_FACTOR = 4;	/* JPEG limit on sampling factors */
	int DCTSIZE2 = 64;

	int[] jpeg_aritab;

	String JERR_CANT_SUSPEND = "JERR_CANT_SUSPEND";
	String JWRN_ARITH_BAD_CODE = "JWRN_ARITH_BAD_CODE";
	String JERR_NO_ARITH_TABLE = "JERR_NO_ARITH_TABLE";
	String JWRN_NOT_SEQUENTIAL = "JWRN_NOT_SEQUENTIAL";
	String JERR_BAD_PROGRESSION = "JERR_BAD_PROGRESSION";
	String JWRN_BOGUS_PROGRESSION = "JWRN_BOGUS_PROGRESSION";
void ERREXIT(Object... o)
{
	System.out.println(o);
	System.exit(-1);
}
void ERREXIT1(Object... o)
{
	System.out.println(o);
	System.exit(-1);
}
void ERREXIT4(Object... o)
{
	System.out.println(o);
	System.exit(-1);
}
void WARNMS(Object... o)
{
	System.out.println(o);
	System.exit(-1);
}
void WARNMS2(Object... o)
{
	System.out.println(o);
	System.exit(-1);
}

	void MEMZERO(int[] arr, int off, int len)
	{
		Arrays.fill(arr, off, off+len, 0);
	}
	int IRIGHT_SHIFT(int n, int q)
	{
		return n >> q;
	}
	class JBLOCKROW
	{
		int[] data;
	}

	class arith_entropy_ptr
	{
		int decode_mcu;
		int a;
		int c;
		int ct;
		int[] last_dc_val;
		int[] dc_context;
		int[][] dc_stats;
		int restarts_to_go;
		int[][] ac_stats;
		int[] fixed_bin;
	}

	class j_decompress_ptr
	{

		int Al;
		int[] natural_order;
		int[] arith_ac_K;
		int blocks_in_MCU;
		int[] MCU_membership;
		int Ah;
		int restart_interval;
		int Ss;
		int Se;
		int Sh;
		int lim_Se;
		boolean progressive_mode;
		int comps_in_scan;
		jpeg_component_info[] cur_comp_info;
		arith_entropy_ptr entropy;
		int unread_marker;
		int[] arith_dc_L;
		int[] arith_dc_U;
		int[][] coef_bits;
		int num_components;
	}
	class jpeg_component_info
	{
		int dc_tbl_no;
		int ac_tbl_no;
		int component_index;
	}

class arith_entropy_decoder
{
  int c;       /* C register, base of coding interval + input bit buffer */
  int a;               /* A register, normalized size of coding interval */
  int ct;     /* bit shift counter, # of bits left in bit buffer part of C */
                                                         /* init: ct = -16 */
                                                         /* run: ct = 0..7 */
                                                         /* error: ct = -1 */
  int[] last_dc_val = new int[MAX_COMPS_IN_SCAN]; /* last DC coef for each component */
  int[] dc_context = new int[MAX_COMPS_IN_SCAN]; /* context index for DC conditioning */

  int restarts_to_go;	/* MCUs left in this restart interval */

  /* Pointers to statistics areas (these workspaces have image lifespan) */
  int[] dc_stats = new int[NUM_ARITH_TBLS];
  int[] ac_stats = new int[NUM_ARITH_TBLS];

  /* Statistics bin for coding with fixed probability 0.5 */
  int[] fixed_bin = new int[4];
}

/* The following two definitions specify the allocation chunk size
 * for the statistics area.
 * According to sections F.1.4.4.1.3 and F.1.4.4.2, we need at least
 * 49 statistics bins for DC, and 245 statistics bins for AC coding.
 *
 * We use a compact representation with 1 byte per statistics bin,
 * thus the numbers directly represent byte sizes.
 * This 1 byte per statistics bin contains the meaning of the MPS
 * (more probable symbol) in the highest bit (mask 0x80), and the
 * index into the probability estimation state machine table
 * in the lower bits (mask 0x7F).
 */

int DC_STAT_BINS = 64;
int AC_STAT_BINS = 256;


int get_byte (j_decompress_ptr cinfo) throws IOException
/* Read next input byte; we do not support suspension in this module. */
{
  return mInputStream.read();
}


/*
 * The core arithmetic decoding routine (common in JPEG and JBIG).
 * This needs to go as fast as possible.
 * Machine-dependent optimization facilities
 * are not utilized in this portable implementation.
 * However, this code should be fairly efficient and
 * may be a good base for further optimizations anyway.
 *
 * Return value is 0 or 1 (binary decision).
 *
 * Note: I've changed the handling of the code base & bit
 * buffer register C compared to other implementations
 * based on the standards layout & procedures.
 * While it also contains both the actual base of the
 * coding interval (16 bits) and the next-bits buffer,
 * the cut-point between these two parts is floating
 * (instead of fixed) with the bit shift counter CT.
 * Thus, we also need only one (variable instead of
 * fixed size) shift for the LPS/MPS decision, and
 * we can do away with any renormalization update
 * of C (except for new data insertion, of course).
 *
 * I've also introduced a new scheme for accessing
 * the probability estimation state machine table,
 * derived from Markus Kuhn's JBIG implementation.
 */

int arith_decode (j_decompress_ptr cinfo, int[] st, int st_off) throws IOException
{
  arith_entropy_ptr e = (arith_entropy_ptr) cinfo.entropy;
  int nl, nm;
  int qe, temp;
  int data;
  int[] sv;
  int sv_off;

  /* Renormalization & data input per section D.2.6 */
  while (e.a < 0x8000L) {
    if (--e.ct < 0) {
      /* Need to fetch next data byte */
      if (cinfo.unread_marker!=0)
	data = 0;		/* stuff zero data */
      else {
	data = get_byte(cinfo);	/* read next input byte */
	if (data == 0xFF) {	/* zero stuff or marker code */
	  do data = get_byte(cinfo);
	  while (data == 0xFF);	/* swallow extra 0xFF bytes */
	  if (data == 0)
	    data = 0xFF;	/* discard stuffed zero byte */
	  else {
	    /* Note: Different from the Huffman decoder, hitting
	     * a marker while processing the compressed data
	     * segment is legal in arithmetic coding.
	     * The convention is to supply zero data
	     * then until decoding is complete.
	     */
	    cinfo.unread_marker = data;
	    data = 0;
	  }
	}
      }
      e.c = (e.c << 8) | data; /* insert data into C register */
      if ((e.ct += 8) < 0)	 /* update bit shift counter */
	/* Need more initial bytes */
	if (++e.ct == 0)
	  /* Got 2 initial bytes . re-init A and exit loop */
	  e.a = 0x8000; /* => e.a = 0x10000L after loop exit */
    }
    e.a <<= 1;
  }

  /* Fetch values from our compact representation of Table D.3(D.2):
   * Qe values and probability estimation state machine
   */
  sv = st;
  sv_off = 0;
  qe = jpeg_aritab[sv[sv_off] & 0x7F];	/* => Qe_Value */
  nl = qe & 0xFF; qe >>= 8;	/* Next_Index_LPS + Switch_MPS */
  nm = qe & 0xFF; qe >>= 8;	/* Next_Index_MPS */

  /* Decode & estimation procedures per sections D.2.4 & D.2.5 */
  temp = e.a - qe;
  e.a = temp;
  temp <<= e.ct;
  if (e.c >= temp) {
    e.c -= temp;
    /* Conditional LPS (less probable symbol) exchange */
    if (e.a < qe) {
      e.a = qe;
      sv[sv_off] = (sv[sv_off] & 0x80) ^ nm;	/* Estimate_after_MPS */
    } else {
      e.a = qe;
      sv[sv_off] = (sv[sv_off] & 0x80) ^ nl;	/* Estimate_after_LPS */
      sv_off ^= 0x80;		/* Exchange LPS/MPS */
    }
  } else if (e.a < 0x8000L) {
    /* Conditional MPS (more probable symbol) exchange */
    if (e.a < qe) {
      sv_off = (sv[sv_off] & 0x80) ^ nl;	/* Estimate_after_LPS */
      sv[sv_off] ^= 0x80;		/* Exchange LPS/MPS */
    } else {
      sv[sv_off] = (sv[sv_off] & 0x80) ^ nm;	/* Estimate_after_MPS */
    }
  }

  return sv[sv_off] >> 7;
}


/*
 * Check for a restart marker & resynchronize decoder.
 */

void
process_restart (j_decompress_ptr cinfo)
{
  arith_entropy_ptr entropy = (arith_entropy_ptr) cinfo.entropy;
  int ci;
  jpeg_component_info compptr;

  /* Advance past the RSTn marker */
//  if (! (cinfo.marker.read_restart_marker) (cinfo))
//    ERREXIT(cinfo, JERR_CANT_SUSPEND);

  /* Re-initialize statistics areas */
  for (ci = 0; ci < cinfo.comps_in_scan; ci++) {
    compptr = cinfo.cur_comp_info[ci];
    if (! cinfo.progressive_mode || (cinfo.Ss == 0 && cinfo.Ah == 0)) {
		for (int[] stats : entropy.dc_stats)
      MEMZERO(stats, compptr.dc_tbl_no, DC_STAT_BINS);
      /* Reset DC predictions to 0 */
      entropy.last_dc_val[ci] = 0;
      entropy.dc_context[ci] = 0;
    }
    if ((! cinfo.progressive_mode && cinfo.lim_Se!=0) ||
	(cinfo.progressive_mode && cinfo.Ss!=0)) {
		for (int[] stats : entropy.ac_stats)
      MEMZERO(stats, compptr.ac_tbl_no, AC_STAT_BINS);
    }
  }

  /* Reset arithmetic decoding variables */
  entropy.c = 0;
  entropy.a = 0;
  entropy.ct = -16;	/* force reading 2 initial bytes to fill C */

  /* Reset restart counter */
  entropy.restarts_to_go = cinfo.restart_interval;
}


/*
 * Arithmetic MCU decoding.
 * Each of these routines decodes and returns one MCU's worth of
 * arithmetic-compressed coefficients.
 * The coefficients are reordered from zigzag order into natural array order,
 * but are not dequantized.
 *
 * The i'th block of the MCU is stored into the block pointed to by
 * MCU_data[i].  WE ASSUME THIS AREA IS INITIALLY ZEROED BY THE CALLER.
 */

/*
 * MCU decoding for DC initial scan (either spectral selection,
 * or first pass of successive approximation).
 */

boolean decode_mcu_DC_first (j_decompress_ptr cinfo, JBLOCKROW[] MCU_data) throws IOException
{
  arith_entropy_ptr entropy = (arith_entropy_ptr) cinfo.entropy;
  JBLOCKROW block;
  int[] st;
  int st_off;
  int blkn, ci, tbl, sign;
  int v, m;

  /* Process restart marker if needed */
  if (cinfo.restart_interval!=0) {
    if (entropy.restarts_to_go == 0)
      process_restart(cinfo);
    entropy.restarts_to_go--;
  }

  if (entropy.ct == -1) return true;	/* if error do nothing */

  /* Outer loop handles each block in the MCU */

  for (blkn = 0; blkn < cinfo.blocks_in_MCU; blkn++) {
    block = MCU_data[blkn];
    ci = cinfo.MCU_membership[blkn];
    tbl = cinfo.cur_comp_info[ci].dc_tbl_no;

    /* Sections F.2.4.1 & F.1.4.4.1: Decoding of DC coefficients */

    /* Table F.4: Point to statistics bin S0 for DC coefficient coding */
    st = entropy.dc_stats[tbl];
	st_off = entropy.dc_context[ci];

    /* Figure F.19: Decode_DC_DIFF */
    if (arith_decode(cinfo, st, st_off) == 0)
      entropy.dc_context[ci] = 0;
    else {
      /* Figure F.21: Decoding nonzero value v */
      /* Figure F.22: Decoding the sign of v */
      sign = arith_decode(cinfo, st, st_off + 1);
      st_off += 2; st_off += sign;
      /* Figure F.23: Decoding the magnitude category of v */
      if ((m = arith_decode(cinfo, st, st_off)) != 0) {
	st = entropy.dc_stats[tbl];
	st_off = 20;	/* Table F.4: X1 = 20 */
	while (arith_decode(cinfo, st, st_off)!=0) {
	  if ((m <<= 1) == 0x8000) {
	    WARNMS(cinfo, JWRN_ARITH_BAD_CODE);
	    entropy.ct = -1;			/* magnitude overflow */
	    return true;
	  }
	  st_off += 1;
	}
      }
      /* Section F.1.4.4.1.2: Establish dc_context conditioning category */
      if (m < (int) ((1L << cinfo.arith_dc_L[tbl]) >> 1))
	entropy.dc_context[ci] = 0;		   /* zero diff category */
      else if (m > (int) ((1L << cinfo.arith_dc_U[tbl]) >> 1))
	entropy.dc_context[ci] = 12 + (sign * 4); /* large diff category */
      else
	entropy.dc_context[ci] = 4 + (sign * 4);  /* small diff category */
      v = m;
      /* Figure F.24: Decoding the magnitude bit pattern of v */
      st_off += 14;
      while ((m >>= 1)!=0)
	if (arith_decode(cinfo, st, st_off)!=0) v |= m;
      v += 1; if (sign!=0) v = -v;
      entropy.last_dc_val[ci] += v;
    }

    /* Scale and output the DC coefficient (assumes jpeg_natural_order[0]=0) */
    block.data[0] = (entropy.last_dc_val[ci] << cinfo.Al);
  }

  return true;
}


/*
 * MCU decoding for AC initial scan (either spectral selection,
 * or first pass of successive approximation).
 */

boolean decode_mcu_AC_first (j_decompress_ptr cinfo, JBLOCKROW[] MCU_data) throws IOException
{
  arith_entropy_ptr entropy = (arith_entropy_ptr) cinfo.entropy;
  JBLOCKROW block;
  int[] st;
  int st_off;
  int tbl, sign, k;
  int v, m;
  int[] natural_order;

  /* Process restart marker if needed */
  if (cinfo.restart_interval!=0) {
    if (entropy.restarts_to_go == 0)
      process_restart(cinfo);
    entropy.restarts_to_go--;
  }

  if (entropy.ct == -1) return true;	/* if error do nothing */

  natural_order = cinfo.natural_order;

  /* There is always only one block per MCU */
  block = MCU_data[0];
  tbl = cinfo.cur_comp_info[0].ac_tbl_no;

  /* Sections F.2.4.2 & F.1.4.4.2: Decoding of AC coefficients */

  /* Figure F.20: Decode_AC_coefficients */
  k = cinfo.Ss - 1;
  do {
    st = entropy.ac_stats[tbl];
	st_off = 3 * k;
    if (arith_decode(cinfo, st, st_off)!=0) break;		/* EOB flag */
    for (;;) {
      k++;
      if (arith_decode(cinfo, st,st_off + 1)!=0) break;
      st_off += 3;
      if (k >= cinfo.Se) {
	WARNMS(cinfo, JWRN_ARITH_BAD_CODE);
	entropy.ct = -1;			/* spectral overflow */
	return true;
      }
    }
    /* Figure F.21: Decoding nonzero value v */
    /* Figure F.22: Decoding the sign of v */
    sign = arith_decode(cinfo, entropy.fixed_bin,0);
    st_off += 2;
    /* Figure F.23: Decoding the magnitude category of v */
    if ((m = arith_decode(cinfo, st, st_off)) != 0) {
      if (arith_decode(cinfo, st, st_off)!=0) {
	m <<= 1;
	st = entropy.ac_stats[tbl];
	st_off = (k <= cinfo.arith_ac_K[tbl] ? 189 : 217);
	while (arith_decode(cinfo, st,st_off)!=0) {
	  if ((m <<= 1) == 0x8000) {
	    WARNMS(cinfo, JWRN_ARITH_BAD_CODE);
	    entropy.ct = -1;			/* magnitude overflow */
	    return true;
	  }
	  st_off += 1;
	}
      }
    }
    v = m;
    /* Figure F.24: Decoding the magnitude bit pattern of v */
    st_off += 14;
    while ((m >>= 1)!=0)
      if (arith_decode(cinfo, st,st_off)!=0) v |= m;
    v += 1; if (sign!=0) v = -v;
    /* Scale and output coefficient in natural (dezigzagged) order */
    block.data[natural_order[k]] = (v << cinfo.Al);
  } while (k < cinfo.Se);

  return true;
}


/*
 * MCU decoding for DC successive approximation refinement scan.
 * Note: we assume such scans can be multi-component,
 * although the spec is not very clear on the point.
 */

boolean decode_mcu_DC_refine (j_decompress_ptr cinfo, JBLOCKROW[] MCU_data) throws IOException
{
  arith_entropy_ptr entropy = (arith_entropy_ptr) cinfo.entropy;
  int[] st;
  int st_off;
  int p1, blkn;

  /* Process restart marker if needed */
  if (cinfo.restart_interval!=0) {
    if (entropy.restarts_to_go == 0)
      process_restart(cinfo);
    entropy.restarts_to_go--;
  }

  st = entropy.fixed_bin;	/* use fixed probability estimation */
  p1 = 1 << cinfo.Al;		/* 1 in the bit position being coded */

  /* Outer loop handles each block in the MCU */

  for (blkn = 0; blkn < cinfo.blocks_in_MCU; blkn++) {
    /* Encoded data is simply the next bit of the two's-complement DC value */
    if (arith_decode(cinfo, st,0)!=0)
      MCU_data[blkn].data[0] |= p1; // ?????????????????????????????
  }

  return true;
}


/*
 * MCU decoding for AC successive approximation refinement scan.
 */

boolean decode_mcu_AC_refine (j_decompress_ptr cinfo, JBLOCKROW[] MCU_data) throws IOException
{
  arith_entropy_ptr entropy = (arith_entropy_ptr) cinfo.entropy;
  JBLOCKROW block;
  int thiscoef;
  int[] st;
  int st_off;
  int tbl, k, kex;
  int p1, m1;
  int[] natural_order;

  /* Process restart marker if needed */
  if (cinfo.restart_interval!=0) {
    if (entropy.restarts_to_go == 0)
      process_restart(cinfo);
    entropy.restarts_to_go--;
  }

  if (entropy.ct == -1) return true;	/* if error do nothing */

  natural_order = cinfo.natural_order;

  /* There is always only one block per MCU */
  block = MCU_data[0];
  tbl = cinfo.cur_comp_info[0].ac_tbl_no;

  p1 = 1 << cinfo.Al;		/* 1 in the bit position being coded */
  m1 = (-1) << cinfo.Al;	/* -1 in the bit position being coded */

  /* Establish EOBx (previous stage end-of-block) index */
  kex = cinfo.Se;
  do {
    if (block.data[natural_order[kex]]!=0) break;
  } while (--kex!=0);

  k = cinfo.Ss - 1;
  do {
    st = entropy.ac_stats[tbl];
	st_off = 3 * k;
    if (k >= kex)
      if (arith_decode(cinfo, st,st_off)!=0) break;	/* EOB flag */
    for (;;) {
      thiscoef = natural_order[++k];
      if (block.data[thiscoef]!=0) {				/* previously nonzero coef */
	if (arith_decode(cinfo, st, st_off + 2)!=0) {
	  if (block.data[thiscoef] < 0)
	    block.data[thiscoef] += m1;
	  else
	    block.data[thiscoef] += p1;
	}
	break;
      }
      if (arith_decode(cinfo, st,st_off + 1)!=0) {	/* newly nonzero coef */
	if (arith_decode(cinfo, entropy.fixed_bin,0)!=0)
	  block.data[thiscoef] = m1;
	else
	  block.data[thiscoef] = p1;
	break;
      }
      st_off += 3;
      if (k >= cinfo.Se) {
	WARNMS(cinfo, JWRN_ARITH_BAD_CODE);
	entropy.ct = -1;			/* spectral overflow */
	return true;
      }
    }
  } while (k < cinfo.Se);

  return true;
}


/*
 * Decode one MCU's worth of arithmetic-compressed coefficients.
 */

boolean decode_mcu (j_decompress_ptr cinfo, JBLOCKROW[] MCU_data) throws IOException
{
	switch (cinfo.entropy.decode_mcu)
	{
		case x_decode_mcu_AC_first:
			return decode_mcu_AC_first(cinfo, MCU_data);
		case x_decode_mcu_AC_refine:
			return decode_mcu_AC_refine(cinfo, MCU_data);
		case x_decode_mcu_DC_first:
			return decode_mcu_DC_first(cinfo, MCU_data);
		case x_decode_mcu_DC_refine:
			return decode_mcu_DC_refine(cinfo, MCU_data);
	}

  arith_entropy_ptr entropy = (arith_entropy_ptr) cinfo.entropy;
  jpeg_component_info compptr;
  JBLOCKROW block;
  int[] st;
  int st_off;
  int blkn, ci, tbl, sign, k;
  int v, m;
  int[] natural_order;

  /* Process restart marker if needed */
  if (cinfo.restart_interval!=0) {
    if (entropy.restarts_to_go == 0)
      process_restart(cinfo);
    entropy.restarts_to_go--;
  }

  if (entropy.ct == -1) return true;	/* if error do nothing */

  natural_order = cinfo.natural_order;

  /* Outer loop handles each block in the MCU */

  for (blkn = 0; blkn < cinfo.blocks_in_MCU; blkn++) {
    block = MCU_data[blkn];
    ci = cinfo.MCU_membership[blkn];
    compptr = cinfo.cur_comp_info[ci];

    /* Sections F.2.4.1 & F.1.4.4.1: Decoding of DC coefficients */

    tbl = compptr.dc_tbl_no;

    /* Table F.4: Point to statistics bin S0 for DC coefficient coding */
    st = entropy.dc_stats[tbl];
	st_off = entropy.dc_context[ci];

    /* Figure F.19: Decode_DC_DIFF */
    if (arith_decode(cinfo, st,st_off) == 0)
      entropy.dc_context[ci] = 0;
    else {
      /* Figure F.21: Decoding nonzero value v */
      /* Figure F.22: Decoding the sign of v */
      sign = arith_decode(cinfo, st,st_off + 1);
      st_off += 2; st_off += sign;
      /* Figure F.23: Decoding the magnitude category of v */
      if ((m = arith_decode(cinfo, st,st_off)) != 0) {
	st = entropy.dc_stats[tbl];
	st_off = 20;	/* Table F.4: X1 = 20 */
	while (arith_decode(cinfo, st,st_off)!=0) {
	  if ((m <<= 1) == 0x8000) {
//	    WARNMS(cinfo, JWRN_ARITH_BAD_CODE);
	    entropy.ct = -1;			/* magnitude overflow */
	    return true;
	  }
	  st_off += 1;
	}
      }
      /* Section F.1.4.4.1.2: Establish dc_context conditioning category */
      if (m < (int) ((1L << cinfo.arith_dc_L[tbl]) >> 1))
	entropy.dc_context[ci] = 0;		   /* zero diff category */
      else if (m > (int) ((1L << cinfo.arith_dc_U[tbl]) >> 1))
	entropy.dc_context[ci] = 12 + (sign * 4); /* large diff category */
      else
	entropy.dc_context[ci] = 4 + (sign * 4);  /* small diff category */
      v = m;
      /* Figure F.24: Decoding the magnitude bit pattern of v */
      st_off += 14;
      while ((m >>= 1)!=0)
	if (arith_decode(cinfo, st,st_off)!=0) v |= m;
      v += 1; if (sign!=0) v = -v;
      entropy.last_dc_val[ci] += v;
    }

    block.data[0] = entropy.last_dc_val[ci];

    /* Sections F.2.4.2 & F.1.4.4.2: Decoding of AC coefficients */

    if (cinfo.lim_Se == 0) continue;
    tbl = compptr.ac_tbl_no;
    k = 0;

    /* Figure F.20: Decode_AC_coefficients */
    do {
      st = entropy.ac_stats[tbl];
	  st_off = 3 * k;
      if (arith_decode(cinfo, st,st_off)!=0) break;	/* EOB flag */
      for (;;) {
	k++;
	if (arith_decode(cinfo, st,st_off + 1)!=0) break;
	st_off += 3;
	if (k >= cinfo.lim_Se) {
	  WARNMS(cinfo, JWRN_ARITH_BAD_CODE);
	  entropy.ct = -1;			/* spectral overflow */
	  return true;
	}
      }
      /* Figure F.21: Decoding nonzero value v */
      /* Figure F.22: Decoding the sign of v */
      sign = arith_decode(cinfo, entropy.fixed_bin,0);
      st_off += 2;
      /* Figure F.23: Decoding the magnitude category of v */
      if ((m = arith_decode(cinfo, st,st_off)) != 0) {
	if (arith_decode(cinfo, st,st_off)!=0) {
	  m <<= 1;
	  st = entropy.ac_stats[tbl];
	  st_off = (k <= cinfo.arith_ac_K[tbl] ? 189 : 217);
	  while (arith_decode(cinfo, st,st_off)!=0) {
	    if ((m <<= 1) == 0x8000) {
	      WARNMS(cinfo, JWRN_ARITH_BAD_CODE);
	      entropy.ct = -1;			/* magnitude overflow */
	      return true;
	    }
	    st_off += 1;
	  }
	}
      }
      v = m;
      /* Figure F.24: Decoding the magnitude bit pattern of v */
      st_off += 14;
      while ((m >>= 1)!=0)
	if (arith_decode(cinfo, st,st_off)!=0) v |= m;
      v += 1; if (sign!=0) v = -v;
      block.data[natural_order[k]] = v;
    } while (k < cinfo.lim_Se);
  }

  return true;
}

final static int x_decode_mcu_DC_first=1;
final static int x_decode_mcu_AC_first=2;
final static int x_decode_mcu_DC_refine=3;
final static int x_decode_mcu_AC_refine=4;
final static int x_decode_mcu=0;

/*
 * Initialize for an arithmetic-compressed scan.
 */

void
start_pass (j_decompress_ptr cinfo)
{
  arith_entropy_ptr entropy = (arith_entropy_ptr) cinfo.entropy;
  int ci, tbl;
  jpeg_component_info compptr;

  if (cinfo.progressive_mode) {
    /* Validate progressive scan parameters */
	boolean bad = false;
    if (cinfo.Ss == 0) {
      if (cinfo.Se != 0)
	bad=true;
    } else {
      /* need not check Ss/Se < 0 since they came from unsigned bytes */
      if (cinfo.Se < cinfo.Ss || cinfo.Se > cinfo.lim_Se)
	bad=true;
      /* AC scans may have only one component */
      if (cinfo.comps_in_scan != 1)
	bad=true;
    }
    if (cinfo.Ah != 0) {
      /* Successive approximation refinement scan: must have Al = Ah-1. */
      if (cinfo.Ah-1 != cinfo.Al)
	bad=true;
    }
    if (bad || cinfo.Al > 13) {	/* need not check for < 0 */
      ERREXIT4(cinfo, JERR_BAD_PROGRESSION,
	       cinfo.Ss, cinfo.Se, cinfo.Ah, cinfo.Al);
    }
    /* Update progression status, and verify that scan order is legal.
     * Note that inter-scan inconsistencies are treated as warnings
     * not fatal errors ... not clear if this is right way to behave.
     */
    for (ci = 0; ci < cinfo.comps_in_scan; ci++) {
      int coefi, cindex = cinfo.cur_comp_info[ci].component_index;
      int[] coef_bit_ptr = cinfo.coef_bits[cindex];
      if (cinfo.Ss!=0 && coef_bit_ptr[0] < 0) /* AC without prior DC scan */
	WARNMS2(cinfo, JWRN_BOGUS_PROGRESSION, cindex, 0);
      for (coefi = cinfo.Ss; coefi <= cinfo.Se; coefi++) {
	int expected = (coef_bit_ptr[coefi] < 0) ? 0 : coef_bit_ptr[coefi];
	if (cinfo.Ah != expected)
	  WARNMS2(cinfo, JWRN_BOGUS_PROGRESSION, cindex, coefi);
	coef_bit_ptr[coefi] = cinfo.Al;
      }
    }
    /* Select MCU decoding routine */
    if (cinfo.Ah == 0) {
      if (cinfo.Ss == 0)
	entropy.decode_mcu = x_decode_mcu_DC_first;
      else
	entropy.decode_mcu = x_decode_mcu_AC_first;
    } else {
      if (cinfo.Ss == 0)
	entropy.decode_mcu = x_decode_mcu_DC_refine;
      else
	entropy.decode_mcu = x_decode_mcu_AC_refine;
    }
  } else {
    /* Check that the scan parameters Ss, Se, Ah/Al are OK for sequential JPEG.
     * This ought to be an error condition, but we make it a warning.
     */
    if (cinfo.Ss != 0 || cinfo.Ah != 0 || cinfo.Al != 0 ||
	(cinfo.Se < DCTSIZE2 && cinfo.Se != cinfo.lim_Se))
      WARNMS(cinfo, JWRN_NOT_SEQUENTIAL);
    /* Select MCU decoding routine */
    entropy.decode_mcu = x_decode_mcu;
  }

  /* Allocate & initialize requested statistics areas */
  for (ci = 0; ci < cinfo.comps_in_scan; ci++) {
    compptr = cinfo.cur_comp_info[ci];
    if (! cinfo.progressive_mode || (cinfo.Ss == 0 && cinfo.Ah == 0)) {
      tbl = compptr.dc_tbl_no;
      if (tbl < 0 || tbl >= NUM_ARITH_TBLS)
	ERREXIT1(cinfo, JERR_NO_ARITH_TABLE, tbl);
      if (entropy.dc_stats[tbl] == null)
	entropy.dc_stats[tbl] = new int[DC_STAT_BINS];
      /* Initialize DC predictions to 0 */
      entropy.last_dc_val[ci] = 0;
      entropy.dc_context[ci] = 0;
    }
    if ((! cinfo.progressive_mode && cinfo.lim_Se!=0) ||
	(cinfo.progressive_mode && cinfo.Ss!=0)) {
      tbl = compptr.ac_tbl_no;
      if (tbl < 0 || tbl >= NUM_ARITH_TBLS)
	ERREXIT1(cinfo, JERR_NO_ARITH_TABLE, tbl);
      if (entropy.ac_stats[tbl] == null)
	entropy.ac_stats[tbl] = new int[AC_STAT_BINS];
    }
  }

  /* Initialize arithmetic decoding variables */
  entropy.c = 0;
  entropy.a = 0;
  entropy.ct = -16;	/* force reading 2 initial bytes to fill C */

  /* Initialize restart counter */
  entropy.restarts_to_go = cinfo.restart_interval;
}


/*
 * Finish up at the end of an arithmetic-compressed scan.
 */

void
finish_pass (j_decompress_ptr cinfo)
{
  /* no work necessary here */
}


/*
 * Module initialization routine for arithmetic entropy decoding.
 */

void
jinit_arith_decoder (j_decompress_ptr cinfo)
{
  arith_entropy_ptr entropy = new arith_entropy_ptr();
  int i;

//  entropy = (arith_entropy_ptr)  (*cinfo.mem.alloc_small) ((j_common_ptr) cinfo, JPOOL_IMAGE,SIZEOF(arith_entropy_decoder));
//  cinfo.entropy = &entropy.pub;
//  entropy.pub.start_pass = start_pass;
//  entropy.pub.finish_pass = finish_pass;

  /* Mark tables unallocated */
  for (i = 0; i < NUM_ARITH_TBLS; i++) {
    entropy.dc_stats[i] = null;
    entropy.ac_stats[i] = null;
  }

  /* Initialize index for fixed probability estimation */
  entropy.fixed_bin[0] = 113;

  if (cinfo.progressive_mode) {
    /* Create progression status table */
    int[] coef_bit_ptr;
	int ci;
    cinfo.coef_bits = new int[cinfo.num_components][DCTSIZE2];
    coef_bit_ptr = cinfo.coef_bits[0];
    for (ci = 0; ci < cinfo.num_components; ci++)
      for (i = 0; i < DCTSIZE2; i++)
	coef_bit_ptr[i] = -1;
  }
}
}
