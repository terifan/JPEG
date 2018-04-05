package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import java.util.Arrays;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGEntropyState;
import static org.terifan.imageio.jpeg.JPEGConstants.DCTSIZE2;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;
import static org.terifan.imageio.jpeg.JPEGConstants.jpeg_aritab;


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
public class ArithmeticDecoder extends Decoder
{
	public ArithmeticDecoder(BitInputStream aBitStream)
	{
		super(aBitStream);
	}

	String JERR_CANT_SUSPEND = "JERR_CANT_SUSPEND";
	String JWRN_ARITH_BAD_CODE = "JWRN_ARITH_BAD_CODE";
	String JERR_NO_ARITH_TABLE = "JERR_NO_ARITH_TABLE";
	String JWRN_NOT_SEQUENTIAL = "JWRN_NOT_SEQUENTIAL";
	String JERR_BAD_PROGRESSION = "JERR_BAD_PROGRESSION";
	String JWRN_BOGUS_PROGRESSION = "JWRN_BOGUS_PROGRESSION";


	private void ERREXIT(Object... o)
	{
		throw new IllegalStateException("" + Arrays.asList(o));
	}


	private void WARNMS(Object... o)
	{
		throw new IllegalStateException("" + Arrays.asList(o));
	}


	/* The following two definitions specify the allocation chunk size
	 * for the statistics area.
	 * According to sections F.1.4.4.1.3 and F.1.4.4.2, we need at least
	 * 49 statistics bins for DC, and 245 statistics bins for AC coding.
	 *
	 * We use a compact representation with 1 byte per statistics bin,
	 *  thus the numbers directly represent byte sizes.
	 * This 1 byte per statistics bin contains the meaning of the MPS
	 * (more probable symbol) in the highest bit (mask 0x80), and the
	 * index into the probability estimation state machine table
	 * in the lower bits (mask 0x7F).
	 */
	int DC_STAT_BINS = 64;
	int AC_STAT_BINS = 256;


	/*
	 * The core arithmetic decoding routine (common in JPEG and JBIG).
	 * This needs to go as fast as possible.
	 * Machine-dependent optimization facilities
	 *  are not utilized in this portable implementation.
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
	int arith_decode(JPEG cinfo, final int[] st, final int st_off) throws IOException
	{
		JPEGEntropyState entropy = cinfo.entropy;
		int nl, nm;
		long qe;
		long temp;
		int data;
		int sv;

		/* Renormalization & data input per section D.2.6 */
		while (entropy.a < 0x8000)
		{
			if (--entropy.ct < 0)
			{
				/* Need to fetch next data byte */
				if (mBitStream.getUnreadMarker() != 0)
				{
					data = 0;
					/* stuff zero data */
				}
				else
				{
					data = mBitStream.readInt8();
					/* read next input byte */
					if (data == 0xFF)
					{
						/* zero stuff or marker code */
						do
						{
							data = mBitStream.readInt8();
						}
						while (data == 0xFF);
						/* swallow extra 0xFF bytes */
						if (data == 0)
						{
							data = 0xFF;
							/* discard stuffed zero byte */
						}
						else
						{
							/* Note: Different from the Huffman decoder, hitting
							* a marker while processing the compressed data
							* segment is legal in arithmetic coding.
							* The convention is to supply zero data
							* then until decoding is complete.
							 */
							mBitStream.setUnreadMarker(data);
							data = 0;
						}
					}
				}

				entropy.c = (entropy.c << 8) | data;
				/* insert data into C register */
				if ((entropy.ct += 8) < 0)
				/* update bit shift counter */
				/* Need more initial bytes */
				{
					if (++entropy.ct == 0)
					/* Got 2 initial bytes . re-init A and exit loop */
					{
						entropy.a = 0x8000;
						/* => e.a = 0x10000L after loop exit */
					}
				}
			}
			entropy.a <<= 1;
		}

		/* Fetch values from our compact representation of Table D.3(D.2):
		 * Qe values and probability estimation state machine
		 */
		sv = st[st_off];
		qe = jpeg_aritab[sv & 0x7F];
		/* => Qe_Value */
		nl = (int)(qe & 0xFF);
		qe >>= 8;
		/* Next_Index_LPS + Switch_MPS */
		nm = (int)(qe & 0xFF);
		qe >>= 8;
		/* Next_Index_MPS */

		/* Decode & estimation procedures per sections D.2.4 & D.2.5 */
		temp = entropy.a - qe;
		entropy.a = temp;
		temp <<= entropy.ct;
		if (entropy.c >= temp)
		{
			entropy.c -= temp;
			/* Conditional LPS (less probable symbol) exchange */
			if (entropy.a < qe)
			{
				entropy.a = qe;
				st[st_off] = (sv & 0x80) ^ nm;
				/* Estimate_after_MPS */
			}
			else
			{
				entropy.a = qe;
				st[st_off] = (sv & 0x80) ^ nl;
				/* Estimate_after_LPS */
				sv ^= 0x80;
				/* Exchange LPS/MPS */
			}
		}
		else if (entropy.a < 0x8000L)
		{
			/* Conditional MPS (more probable symbol) exchange */
			if (entropy.a < qe)
			{
				st[st_off] = (sv & 0x80) ^ nl;
				/* Estimate_after_LPS */
				sv ^= 0x80;
				/* Exchange LPS/MPS */
			}
			else
			{
				st[st_off] = (sv & 0x80) ^ nm;
				/* Estimate_after_MPS */
			}
		}
	
		return sv >> 7;
	}


	/*
	 * Check for a restart marker & resynchronize decoder.
	 */
	private void process_restart(JPEG cinfo)
	{
		JPEGEntropyState entropy = cinfo.entropy;
		ComponentInfo compptr;

		/* Advance past the RSTn marker */
		//  if (! (cinfo.marker.read_restart_marker) (cinfo))
		//    ERREXIT(cinfo, JERR_CANT_SUSPEND);

		/* Re-initialize statistics areas */
		for (int ci = 0; ci < cinfo.comps_in_scan; ci++)
		{
			compptr = cinfo.cur_comp_info[ci];
			if (!cinfo.mProgressive || (cinfo.Ss == 0 && cinfo.Ah == 0))
			{
				Arrays.fill(entropy.dc_stats[compptr.getTableDC()], 0);
				/* Reset DC predictions to 0 */
				entropy.last_dc_val[ci] = 0;
				entropy.dc_context[ci] = 0;
			}
			if ((!cinfo.mProgressive && cinfo.lim_Se != 0) || (cinfo.mProgressive && cinfo.Ss != 0))
			{
				Arrays.fill(entropy.ac_stats[compptr.getTableAC()], 0);
			}
		}

		/* Reset arithmetic decoding variables */
		entropy.c = 0;
		entropy.a = 0;
		entropy.ct = -16;
		/* force reading 2 initial bytes to fill C */

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
	boolean decode_mcu_DC_first(JPEG cinfo, int[][] MCU_data) throws IOException
	{
		JPEGEntropyState entropy = cinfo.entropy;
		int[] block;
		int[] st;
		int st_off;
		int blkn, ci, tbl, sign;
		int v, m;
		
		/* Process restart marker if needed */
		if (cinfo.restart_interval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				process_restart(cinfo);
			}
			entropy.restarts_to_go--;
		}

		if (entropy.ct == -1)
		{
			return true;
			/* if error do nothing */
		}

		/* Outer loop handles each block in the MCU */
		for (blkn = 0; blkn < cinfo.blocks_in_MCU; blkn++)
		{
			block = MCU_data[blkn];
			ci = cinfo.MCU_membership[blkn];
			tbl = cinfo.cur_comp_info[ci].getTableDC();

			/* Sections F.2.4.1 & F.1.4.4.1: Decoding of DC coefficients */

			/* Table F.4: Point to statistics bin S0 for DC coefficient coding */
			st = entropy.dc_stats[tbl];
			st_off = entropy.dc_context[ci];

			/* Figure F.19: Decode_DC_DIFF */
			if (arith_decode(cinfo, st, st_off) == 0)
			{
				entropy.dc_context[ci] = 0;
			}
			else
			{
				/* Figure F.21: Decoding nonzero value v */
				/* Figure F.22: Decoding the sign of v */
				sign = arith_decode(cinfo, st, st_off + 1);
				st_off += 2;
				st_off += sign;
				/* Figure F.23: Decoding the magnitude category of v */
				if ((m = arith_decode(cinfo, st, st_off)) != 0)
				{
					st = entropy.dc_stats[tbl];
					st_off = 20;
					/* Table F.4: X1 = 20 */
					while (arith_decode(cinfo, st, st_off) != 0)
					{
						if ((m <<= 1) == 0x8000)
						{
							WARNMS(cinfo, JWRN_ARITH_BAD_CODE + " - 1");
							entropy.ct = -1;
							/* magnitude overflow */
							return true;
						}
						st_off += 1;
					}
				}
				/* Section F.1.4.4.1.2: Establish dc_context conditioning category */
				if (m < ((1 << cinfo.arith_dc_L[tbl]) >> 1))
				{
					entropy.dc_context[ci] = 0;
					/* zero diff category */
				}
				else if (m > ((1 << cinfo.arith_dc_U[tbl]) >> 1))
				{
					entropy.dc_context[ci] = 12 + (sign * 4);
					/* large diff category */
				}
				else
				{
					entropy.dc_context[ci] = 4 + (sign * 4);
					/* small diff category */
				}
				v = m;
				/* Figure F.24: Decoding the magnitude bit pattern of v */
				st_off += 14;
				while ((m >>= 1) != 0)
				{
					if (arith_decode(cinfo, st, st_off) != 0)
					{
						v |= m;
					}
				}
				v += 1;
				if (sign != 0)
				{
					v = -v;
				}
				entropy.last_dc_val[ci] += v;
			}

			/* Scale and output the DC coefficient (assumes jpeg_natural_order[0]=0) */
			block[0] = (entropy.last_dc_val[ci] << cinfo.Al);
		}

		return true;
	}


	/*
	 * MCU decoding for AC initial scan (either spectral selection,
	 * or first pass of successive approximation).
	 */
	boolean decode_mcu_AC_first(JPEG cinfo, int[][] MCU_data) throws IOException
	{
		JPEGEntropyState entropy = cinfo.entropy;
		int[] block;
		int[] st;
		int st_off;
		int tbl, sign, k;
		int v, m;

		/* Process restart marker if needed */
		if (cinfo.restart_interval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				process_restart(cinfo);
			}
			entropy.restarts_to_go--;
		}

		if (entropy.ct == -1)
		{
			return true;
			/* if error do nothing */
		}

		/* There is always only one block per MCU */
		block = MCU_data[0];
		tbl = cinfo.cur_comp_info[0].getTableAC();

		/* Sections F.2.4.2 & F.1.4.4.2: Decoding of AC coefficients */

		/* Figure F.20: Decode_AC_coefficients */
		k = cinfo.Ss - 1;
		do
		{
			st = entropy.ac_stats[tbl];
			st_off = 3 * k;
			if (arith_decode(cinfo, st, st_off) != 0)
			{
				break;
				/* EOB flag */
			}
			for (;;)
			{
				k++;
				if (arith_decode(cinfo, st, st_off + 1) != 0)
				{
					break;
				}
				st_off += 3;
				if (k >= cinfo.Se)
				{
					throw new IllegalStateException("JWRN_ARITH_BAD_CODE - 2: " + k + ">=" + cinfo.Se);
//					entropy.ct = -1;
					/* spectral overflow */
//					return true;
				}
			}
			/* Figure F.21: Decoding nonzero value v */
			/* Figure F.22: Decoding the sign of v */
			sign = arith_decode(cinfo, entropy.fixed_bin, 0);
			st_off += 2;
			/* Figure F.23: Decoding the magnitude category of v */
			if ((m = arith_decode(cinfo, st, st_off)) != 0)
			{
				if (arith_decode(cinfo, st, st_off) != 0)
				{
					m <<= 1;
					st = entropy.ac_stats[tbl];
					st_off = (k <= cinfo.arith_ac_K[tbl] ? 189 : 217);
					while (arith_decode(cinfo, st, st_off) != 0)
					{
						if ((m <<= 1) == 0x8000)
						{
							throw new IllegalStateException("JWRN_ARITH_BAD_CODE - 3");
//							entropy.ct = -1;
							/* magnitude overflow */
//							return true;
						}
						st_off += 1;
					}
				}
			}
			v = m;
			/* Figure F.24: Decoding the magnitude bit pattern of v */
			st_off += 14;
			while ((m >>= 1) != 0)
			{
				if (arith_decode(cinfo, st, st_off) != 0)
				{
					v |= m;
				}
			}
			v += 1;
			if (sign != 0)
			{
				v = -v;
			}
			/* Scale and output coefficient in natural (dezigzagged) order */
			block[NATURAL_ORDER[k]] = v << cinfo.Al;
		}
		while (k < cinfo.Se);

		return true;
	}


	/*
	 * MCU decoding for DC successive approximation refinement scan.
	 * Note: we assume such scans can be multi-component,
	 * although the spec is not very clear on the point.
	 */
	boolean decode_mcu_DC_refine(JPEG cinfo, int[][] MCU_data) throws IOException
	{
		JPEGEntropyState entropy = cinfo.entropy;
		int[] st;
		int p1, blkn;

		/* Process restart marker if needed */
		if (cinfo.restart_interval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				process_restart(cinfo);
			}
			entropy.restarts_to_go--;
		}

		st = entropy.fixed_bin;
		/* use fixed probability estimation */
		p1 = 1 << cinfo.Al;
		/* 1 in the bit position being coded */

		/* Outer loop handles each block in the MCU */
		for (blkn = 0; blkn < cinfo.blocks_in_MCU; blkn++)
		{
			/* Encoded data is simply the next bit of the two's-complement DC value */
			if (arith_decode(cinfo, st, 0) != 0)
			{
				MCU_data[blkn][0] |= p1;
			}
		}

		return true;
	}


	/*
	 * MCU decoding for AC successive approximation refinement scan.
	 */
	boolean decode_mcu_AC_refine(JPEG cinfo, int[][] MCU_data) throws IOException
	{
		JPEGEntropyState entropy = cinfo.entropy;
		int[] block;
		int thiscoef;
		int[] st;
		int st_off;
		int tbl, kex;
		int p1, m1;

		/* Process restart marker if needed */
		if (cinfo.restart_interval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				process_restart(cinfo);
			}
			entropy.restarts_to_go--;
		}

		if (entropy.ct == -1)
		{
			return true;
			/* if error do nothing */
		}

		/* There is always only one block per MCU */
		block = MCU_data[0];
		tbl = cinfo.cur_comp_info[0].getTableAC();

		p1 = 1 << cinfo.Al; // 1 in the bit position being coded
		m1 = -1 << cinfo.Al; // -1 in the bit position being coded

		// Establish EOBx (previous stage end-of-block) index
		kex = cinfo.Se;
		do
		{
			if (block[NATURAL_ORDER[kex]] != 0)
			{
				break;
			}
		}
		while (--kex != 0);

		int k = cinfo.Ss - 1;
		do
		{
			st = entropy.ac_stats[tbl];
			st_off = 3 * k;
			if (k >= kex)
			{
				if (arith_decode(cinfo, st, st_off) != 0)
				{
					break;
					/* EOB flag */
				}
			}
			for (;;)
			{
				thiscoef = NATURAL_ORDER[++k];
				if (block[thiscoef] != 0)
				{
					/* previously nonzero coef */
					if (arith_decode(cinfo, st, st_off + 2) != 0)
					{
						if (block[thiscoef] < 0)
						{
							block[thiscoef] += m1;
						}
						else
						{
							block[thiscoef] += p1;
						}
					}
					break;
				}
				if (arith_decode(cinfo, st, st_off + 1) != 0)
				{
					/* newly nonzero coef */
					if (arith_decode(cinfo, entropy.fixed_bin, 0) != 0)
					{
						block[thiscoef] = m1;
					}
					else
					{
						block[thiscoef] = p1;
					}
					break;
				}
				st_off += 3;
				if (k >= cinfo.Se)
				{
					WARNMS(cinfo, JWRN_ARITH_BAD_CODE + " - 4 " + k + " >= " + cinfo.Se + " " + tbl);
					entropy.ct = -1;
					/* spectral overflow */
					return true;
				}
			}
		}
		while (k < cinfo.Se);

		return true;
	}


	private boolean x(JPEG cinfo, int[][] MCU_data) throws IOException
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

		throw new IllegalStateException();
	}


	/*
	 * Decode one MCU's worth of arithmetic-compressed coefficients.
	 */
	@Override
	boolean decodeMCU(JPEG cinfo, int[][] MCU_data) throws IOException
	{
		if (cinfo.entropy.decode_mcu != x_decode_mcu)
		{
			if (!x(cinfo, MCU_data))
			{
				throw new IllegalStateException("bad code");
			}
			return true;
		}

		for (int[] d : MCU_data)
		{
			Arrays.fill(d, 0);
		}

		JPEGEntropyState entropy = cinfo.entropy;
		ComponentInfo compptr;
		int[] block;
		int[] st;
		int st_off;
		int blkn, ci, tbl, sign, k;
		int v, m;

		/* Process restart marker if needed */
		if (cinfo.restart_interval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				process_restart(cinfo);
			}
			entropy.restarts_to_go--;
		}

		if (entropy.ct == -1)
		{
			return true;
			/* if error do nothing */
		}

		/* Outer loop handles each block in the MCU */
		for (blkn = 0; blkn < cinfo.blocks_in_MCU; blkn++)
		{
			block = MCU_data[blkn];
			ci = cinfo.MCU_membership[blkn];
			compptr = cinfo.cur_comp_info[ci];

			/* Sections F.2.4.1 & F.1.4.4.1: Decoding of DC coefficients */
			tbl = compptr.getTableDC();

			/* Table F.4: Point to statistics bin S0 for DC coefficient coding */
			st = entropy.dc_stats[tbl];
			st_off = entropy.dc_context[ci];

			/* Figure F.19: Decode_DC_DIFF */
			if (arith_decode(cinfo, st, st_off) == 0)
			{
				entropy.dc_context[ci] = 0;
			}
			else
			{
				/* Figure F.21: Decoding nonzero value v */
				/* Figure F.22: Decoding the sign of v */
				sign = arith_decode(cinfo, st, st_off + 1);
				st_off += 2;
				st_off += sign;
				/* Figure F.23: Decoding the magnitude category of v */
				if ((m = arith_decode(cinfo, st, st_off)) != 0)
				{
					st = entropy.dc_stats[tbl];
					st_off = 20;
					/* Table F.4: X1 = 20 */
					while (arith_decode(cinfo, st, st_off) != 0)
					{
						if ((m <<= 1) == 0x8000)
						{
							//WARNMS(cinfo, JWRN_ARITH_BAD_CODE+" - 5");
							entropy.ct = -1;
							/* magnitude overflow */
							return true;
						}
						st_off += 1;
					}
				}
				/* Section F.1.4.4.1.2: Establish dc_context conditioning category */
				if (m < ((1 << cinfo.arith_dc_L[tbl]) >> 1))
				{
					entropy.dc_context[ci] = 0;
					/* zero diff category */
				}
				else if (m > ((1 << cinfo.arith_dc_U[tbl]) >> 1))
				{
					entropy.dc_context[ci] = 12 + (sign * 4);
					/* large diff category */
				}
				else
				{
					entropy.dc_context[ci] = 4 + (sign * 4);
					/* small diff category */
				}
				v = m;
				/* Figure F.24: Decoding the magnitude bit pattern of v */
				st_off += 14;
				while ((m >>= 1) != 0)
				{
					if (arith_decode(cinfo, st, st_off) != 0)
					{
						v |= m;
					}
				}
				v += 1;
				if (sign != 0)
				{
					v = -v;
				}
				entropy.last_dc_val[ci] += v;
			}

			block[0] = entropy.last_dc_val[ci];

			/* Sections F.2.4.2 & F.1.4.4.2: Decoding of AC coefficients */
			if (cinfo.lim_Se == 0)
			{
				continue;
			}
			tbl = compptr.getTableAC();
			k = 0;

			/* Figure F.20: Decode_AC_coefficients */
			do
			{
				st = entropy.ac_stats[tbl];
				st_off = 3 * k;
				if (arith_decode(cinfo, st, st_off) != 0)
				{
					break;
					/* EOB flag */
				}
				for (;;)
				{
					k++;
					if (arith_decode(cinfo, st, st_off + 1) != 0)
					{
						break;
					}
					st_off += 3;
					if (k >= cinfo.lim_Se)
					{
						WARNMS(cinfo, JWRN_ARITH_BAD_CODE + " - 6 " + k + " >= " + cinfo.lim_Se);
						entropy.ct = -1;
						/* spectral overflow */
						return true;
					}
				}
				/* Figure F.21: Decoding nonzero value v */
				/* Figure F.22: Decoding the sign of v */
				sign = arith_decode(cinfo, entropy.fixed_bin, 0);
				st_off += 2;
				/* Figure F.23: Decoding the magnitude category of v */
				if ((m = arith_decode(cinfo, st, st_off)) != 0)
				{
					if (arith_decode(cinfo, st, st_off) != 0)
					{
						m <<= 1;
						st = entropy.ac_stats[tbl];
						st_off = (k <= cinfo.arith_ac_K[tbl] ? 189 : 217);
						while (arith_decode(cinfo, st, st_off) != 0)
						{
							if ((m <<= 1) == 0x8000)
							{
								WARNMS(cinfo, JWRN_ARITH_BAD_CODE + " - 7");
								entropy.ct = -1;
								/* magnitude overflow */
								return true;
							}
							st_off += 1;
						}
					}
				}
				v = m;
				/* Figure F.24: Decoding the magnitude bit pattern of v */
				st_off += 14;
				while ((m >>= 1) != 0)
				{
					if (arith_decode(cinfo, st, st_off) != 0)
					{
						v |= m;
					}
				}
				v += 1;
				if (sign != 0)
				{
					v = -v;
				}
				block[NATURAL_ORDER[k]] = v;
			}
			while (k < cinfo.lim_Se);
		}

		return true;
	}

	final static int x_decode_mcu_DC_first = 1;
	final static int x_decode_mcu_AC_first = 2;
	final static int x_decode_mcu_DC_refine = 3;
	final static int x_decode_mcu_AC_refine = 4;
	final static int x_decode_mcu = 0;


	/*
	 * Initialize for an arithmetic-compressed scan.
	 */
	@Override
	void startPass(JPEG cinfo)
	{
		JPEGEntropyState entropy = cinfo.entropy;
		int ci, tbl;
		ComponentInfo compptr;

		if (cinfo.mProgressive)
		{
			/* Validate progressive scan parameters */
			if (cinfo.Ss == 0)
			{
				if (cinfo.Se != 0)
				{
					ERREXIT(cinfo, JERR_BAD_PROGRESSION, cinfo.Ss, cinfo.Se, cinfo.Ah, cinfo.Al);
				}
			}
			else
			{
				/* need not check Ss/Se < 0 since they came from unsigned bytes */
				if (cinfo.Se < cinfo.Ss || cinfo.Se > cinfo.lim_Se)
				{
					ERREXIT(cinfo, JERR_BAD_PROGRESSION, cinfo.Ss, cinfo.Se, cinfo.Ah, cinfo.Al);
				}
				/* AC scans may have only one component */
				if (cinfo.comps_in_scan != 1)
				{
					ERREXIT(cinfo, JERR_BAD_PROGRESSION, cinfo.Ss, cinfo.Se, cinfo.Ah, cinfo.Al);
				}
			}
			if (cinfo.Ah != 0)
			{
				/* Successive approximation refinement scan: must have Al = Ah-1. */
				if (cinfo.Ah - 1 != cinfo.Al)
				{
					ERREXIT(cinfo, JERR_BAD_PROGRESSION, cinfo.Ss, cinfo.Se, cinfo.Ah, cinfo.Al);
				}
			}
			if (cinfo.Al > 13)
			{
				/* need not check for < 0 */
				ERREXIT(cinfo, JERR_BAD_PROGRESSION, cinfo.Ss, cinfo.Se, cinfo.Ah, cinfo.Al);
			}
			/* Update progression status, and verify that scan order is legal.
			 * Note that inter-scan inconsistencies are treated as warnings
			 * not fatal errors ... not clear if this is right way to behave.
			 */
			for (ci = 0; ci < cinfo.comps_in_scan; ci++)
			{
				int cindex = cinfo.cur_comp_info[ci].getComponentId() - 1;
				if (cinfo.Ss != 0 && cinfo.coef_bits[cindex][0] < 0) // AC without prior DC scan
				{
					throw new IllegalStateException("JWRN_BOGUS_PROGRESSION - AC without prior DC scan: component: " + cindex + ", 0");
				}
				for (int coefi = cinfo.Ss; coefi <= cinfo.Se; coefi++)
				{
					int expected = (cinfo.coef_bits[cindex][coefi] < 0) ? 0 : cinfo.coef_bits[cindex][coefi];
					if (cinfo.Ah != expected)
					{
						throw new IllegalStateException("JWRN_BOGUS_PROGRESSION - " + cinfo.Ah + " != " + expected +", component " + cindex + ", coefi " + coefi);
					}
					cinfo.coef_bits[cindex][coefi] = cinfo.Al;
				}
			}
			/* Select MCU decoding routine */
			if (cinfo.Ah == 0)
			{
				if (cinfo.Ss == 0)
				{
					entropy.decode_mcu = x_decode_mcu_DC_first;
				}
				else
				{
					entropy.decode_mcu = x_decode_mcu_AC_first;
				}
			}
			else
			{
				if (cinfo.Ss == 0)
				{
					entropy.decode_mcu = x_decode_mcu_DC_refine;
				}
				else
				{
					entropy.decode_mcu = x_decode_mcu_AC_refine;
				}
			}
		}
		else
		{
			/* Check that the scan parameters Ss, Se, Ah/Al are OK for sequential JPEG.
			 * This ought to be an error condition, but we make it a warning.
			 */
			if (cinfo.Ss != 0 || cinfo.Ah != 0 || cinfo.Al != 0 || (cinfo.Se < DCTSIZE2 && cinfo.Se != cinfo.lim_Se))
			{
				WARNMS(cinfo, JWRN_NOT_SEQUENTIAL);
			}
			/* Select MCU decoding routine */
			entropy.decode_mcu = x_decode_mcu;
		}

		/* Allocate & initialize requested statistics areas */
		for (ci = 0; ci < cinfo.comps_in_scan; ci++)
		{
			compptr = cinfo.cur_comp_info[ci];
			if (!cinfo.mProgressive || (cinfo.Ss == 0 && cinfo.Ah == 0))
			{
				tbl = compptr.getTableDC();
				if (tbl < 0 || tbl >= NUM_ARITH_TBLS)
				{
					ERREXIT(cinfo, JERR_NO_ARITH_TABLE, tbl);
				}
				entropy.dc_stats[tbl] = new int[DC_STAT_BINS];
				/* Initialize DC predictions to 0 */
				entropy.last_dc_val[ci] = 0;
				entropy.dc_context[ci] = 0;
			}
			if ((!cinfo.mProgressive && cinfo.lim_Se != 0) || (cinfo.mProgressive && cinfo.Ss != 0))
			{
				tbl = compptr.getTableAC();
				if (tbl < 0 || tbl >= NUM_ARITH_TBLS)
				{
					ERREXIT(cinfo, JERR_NO_ARITH_TABLE, tbl);
				}

				entropy.ac_stats[tbl] = new int[AC_STAT_BINS];
			}
		}

		/* Initialize arithmetic decoding variables */
		entropy.c = 0;
		entropy.a = 0;
		entropy.ct = -16;
		/* force reading 2 initial bytes to fill C */

		/* Initialize restart counter */
		entropy.restarts_to_go = cinfo.restart_interval;
	}


	/*
	 * Finish up at the end of an arithmetic-compressed scan.
	 */
	@Override
	void finishPass(JPEG cinfo)
	{
	}


	/*
	 * Module initialization routine for arithmetic entropy decoding.
	 */
	@Override
	void initialize(JPEG cinfo)
	{
		JPEGEntropyState entropy = new JPEGEntropyState();
		cinfo.entropy = entropy;

		/* Mark tables unallocated */
		for (int i = 0; i < NUM_ARITH_TBLS; i++)
		{
			entropy.dc_stats[i] = null;
			entropy.ac_stats[i] = null;
			entropy.dc_context = new int[DC_STAT_BINS];
			entropy.last_dc_val = new int[cinfo.num_components];
		}

		/* Initialize index for fixed probability estimation */
		entropy.fixed_bin[0] = 113;

		if (cinfo.mProgressive)
		{
			/* Create progression status table */
			cinfo.coef_bits = new int[cinfo.num_components][DCTSIZE2];
			for (int ci = 0; ci < cinfo.num_components; ci++)
			{
				for (int i = 0; i < DCTSIZE2; i++)
				{
					cinfo.coef_bits[ci][i] = -1;
				}
			}
		}
	}
}
