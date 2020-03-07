package org.terifan.imageio.jpeg.encoder;

import java.io.IOException;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGEntropyState;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.JPEGConstants;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import java.util.Arrays;
import org.terifan.imageio.jpeg.HuffmanTable;


/*
 * jchuff.c
 *
 * Copyright (C) 1991-1997, Thomas G. Lane.
 * Modified 2006-2013 by Guido Vollbeding.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains Huffman entropy encoding routines.
 * Both sequential and progressive modes are supported in this single module.
 *
 * Much of the complexity here has to do with supporting output suspension.
 * If the data destination module demands suspension, we want to be able to
 * back up to the start of the current MCU.  To do this, we copy state
 * variables into local working storage, and update them back to the
 * permanent JPEG objects only upon successful completion of an MCU.
 *
 * We do not support output suspension for the progressive JPEG mode, since
 * the library currently does not allow multiple-scan files to be written
 * with output suspension.
 */
public class HuffmanEncoder implements Encoder
{
	final static int x_encode_mcu = 0;
	final static int x_encode_mcu_DC_first = 1;
	final static int x_encode_mcu_AC_first = 2;
	final static int x_encode_mcu_DC_refine = 3;
	final static int x_encode_mcu_AC_refine = 4;

	int MAX_COEF_BITS = 10;


	private OutputStream mOutputStream;


	public HuffmanEncoder(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
	}


	/* Derived data constructed for each Huffman table */
	private static class c_derived_tbl
	{
		int[] ehufco = new int[256];
		/* code for each symbol */
		int[] ehufsi = new int[256];
		/* length of code for each symbol */
		/* If no code has been allocated for a symbol S, ehufsi[S] contains 0 */
	}


	/* Expanded entropy encoder object for Huffman encoding.
	 *
	 * The savable_state subrecord contains fields that change within an MCU,
	 * but must not be updated permanently until we complete the MCU.
	 */
	private static class savable_state
	{
		int put_buffer;
		int put_bits;
		int[] last_dc_val = new int[MAX_COMPS_IN_SCAN];
	};


	private static class huff_entropy_encoder extends JPEGEntropyState
	{
		/* Pointers to derived tables (these workspaces have image lifespan) */
		c_derived_tbl[] dc_derived_tbls = new c_derived_tbl[NUM_HUFF_TBLS];
		c_derived_tbl[] ac_derived_tbls = new c_derived_tbl[NUM_HUFF_TBLS];

		/* Statistics tables for optimization */
		int[][] dc_count_ptrs = new int[NUM_HUFF_TBLS][];
		int[][] ac_count_ptrs = new int[NUM_HUFF_TBLS][];

		/* Following fields used only in progressive mode */

		/* Mode flag: TRUE for optimization, FALSE for actual data output */
		boolean gather_statistics;

		/* next_output_byte/free_in_buffer are local copies of cinfo.dest fields.
		 */
		int next_output_byte_offset;
		byte[] next_output_byte = new byte[16]; 		/* => next byte to write in buffer */
		int free_in_buffer = 16; 		/* # of byte spaces remaining in buffer */
		JPEG cinfo;
		/* link to cinfo (needed for dump_buffer) */

		/* Coding status for AC components */
		int ac_tbl_no;
		/* the table number of the single component */
		int EOBRUN;
		/* run length of EOBs */
		int BE;
		/* # of buffered correction bits before MCU */
		int[] bit_buffer;
		/* buffer for correction bits (1 per char) */
		/* packing correction bits tightly would save some space but cost time... */

		private working_state working_state = new working_state();

		savable_state saved = working_state.cur;
//		savable_state saved = new savable_state();
		/* Bit buffer & DC state at start of MCU */
	}

	/* Working state while writing an MCU (sequential mode).
	 * This struct contains all the fields that are needed by subroutines.
	 */
	private static class working_state
	{
		int next_output_byte_offset;
		byte[] next_output_byte = new byte[16]; 	/* => next byte to write in buffer */
		int free_in_buffer = 16; 	/* # of byte spaces remaining in buffer */

		savable_state cur = new savable_state();
		/* Current bit buffer & DC state */
		JPEG cinfo;
		/* dump_buffer needs access to this */
	}

	/* MAX_CORR_BITS is the number of bits the AC refinement correction-bit
	 * buffer can hold.  Larger sizes may slightly improve compression, but
	 * 1000 is already well into the realm of overkill.
	 * The minimum safe size is 64 bits.
	 */
	private int MAX_CORR_BITS = 1000;


	/*
	 * Compute the derived values for a Huffman table.
	 * This routine also performs some validation checks on the table.
	 */
	private c_derived_tbl jpeg_make_c_derived_tbl(JPEG aJPEG, boolean isDC, int tblno, c_derived_tbl pdtbl)
	{
		HuffmanTable htbl;
		c_derived_tbl dtbl;
		int p, i, l, lastp, si, maxsymbol;
		int[] huffsize = new int[257];
		int[] huffcode = new int[257];
		int code;

		/* Note that huffsize[] and huffcode[] are filled in code-length order,
		 * paralleling the order of the symbols themselves in htbl.huffval[].
		 */

		/* Find the input Huffman table */
		if (tblno < 0 || tblno >= NUM_HUFF_TBLS)
		{
			throw new IllegalStateException("JERR_NO_HUFF_TABLE" + tblno);
		}
		htbl = isDC ? aJPEG.dc_huff_tbl_ptrs[tblno] : aJPEG.ac_huff_tbl_ptrs[tblno];
		if (htbl == null)
		{
			throw new IllegalStateException("JERR_NO_HUFF_TABLE " + tblno);
		}

		/* Allocate a workspace if we haven't already done so. */
//  if (*pdtbl == null)
//    *pdtbl = (c_derived_tbl *)      (*cinfo.mem.alloc_small) ((j_common_ptr) cinfo, JPOOL_IMAGE,				  SIZEOF(c_derived_tbl));
		dtbl = pdtbl != null ? pdtbl : new c_derived_tbl();

		/* Figure C.1: make table of Huffman code length for each symbol */
		p = 0;
		for (l = 1; l <= 16; l++)
		{
			i = (int)htbl.bits[l];
			if (i < 0 || p + i > 256)
			/* protect against table overrun */
			{
				throw new IllegalStateException("JERR_BAD_HUFF_TABLE");
			}
			while (i-- != 0)
			{
				huffsize[p++] = (char)l;
			}
		}
		huffsize[p] = 0;
		lastp = p;

		/* Figure C.2: generate the codes themselves */
		/* We also validate that the counts represent a legal Huffman code tree. */
		code = 0;
		si = huffsize[0];
		p = 0;
		while (huffsize[p] != 0)
		{
			while (((int)huffsize[p]) == si)
			{
				huffcode[p++] = code;
				code++;
			}
			/* code is now 1 more than the last code used for codelength si; but
			 * it must still fit in si bits, since no code is allowed to be all ones.
			 */
			if (code >= (1 << si))
			{
				throw new IllegalStateException("JERR_BAD_HUFF_TABLE");
			}
			code <<= 1;
			si++;
		}

		/* Figure C.3: generate encoding tables */
		/* These are code and size indexed by symbol value */

		/* Set all codeless symbols to have code length 0;
		 * this lets us detect duplicate VAL entries here, and later
		 * allows emit_bits to detect any attempt to emit such symbols.
		 */
//		MEMZERO(dtbl.ehufsi, SIZEOF(dtbl.ehufsi));
		Arrays.fill(dtbl.ehufsi, 0);

		/* This is also a convenient place to check for out-of-range
		 * and duplicated VAL entries.  We allow 0..255 for AC symbols
		 * but only 0..15 for DC.  (We could constrain them further
		 * based on data depth and mode, but this seems enough.)
		 */
		maxsymbol = isDC ? 15 : 255;

		for (p = 0; p < lastp; p++)
		{
			i = htbl.huffval[p];
			if (i < 0 || i > maxsymbol || dtbl.ehufsi[i] != 0)
			{
				throw new IllegalStateException("JERR_BAD_HUFF_TABLE");
			}
			dtbl.ehufco[i] = huffcode[p];
			dtbl.ehufsi[i] = huffsize[p];
		}

		return dtbl;
	}


	/* Outputting bytes to the file.
	 * NB: these must be called only when actually outputting,
	 * that is, entropy.gather_statistics == FALSE.
	 */

	/* Emit a byte, taking 'action' if must suspend. */
	private void emit_byte_s(working_state state, int val) throws IOException
	{
		state.next_output_byte[state.next_output_byte_offset++] = (byte)val;
		if (--state.free_in_buffer == 0)
		{
			dump_buffer_s(state);
		}
	}


	/* Emit a byte */
	private void emit_byte_e(huff_entropy_encoder entropy, int val) throws IOException
	{
		entropy.next_output_byte[entropy.next_output_byte_offset++] = (byte)val;
		if (--entropy.free_in_buffer == 0)
		{
			dump_buffer_e(entropy);
		}
	}


	private void dump_buffer_s(working_state state) throws IOException
	{
		mOutputStream.write(state.next_output_byte, 0, state.next_output_byte_offset);

		state.next_output_byte_offset = 0;
		state.free_in_buffer = state.next_output_byte.length;
	}


	private void dump_buffer_e(huff_entropy_encoder entropy) throws IOException
	{
		mOutputStream.write(entropy.next_output_byte, 0, entropy.next_output_byte_offset);

		entropy.next_output_byte_offset = 0;
		entropy.free_in_buffer = entropy.next_output_byte.length;
	}


	/* Outputting bits to the file */

	/* Only the right 24 bits of put_buffer are used; the valid bits are
	 * left-justified in this part.  At most 16 bits can be passed to emit_bits
	 * in one call, and we never retain more than 7 bits in put_buffer
	 * between calls, so 24 bits are sufficient.
	 */
	private void emit_bits_s(working_state state, int code, int size) throws IOException
	{
		/* This routine is heavily used, so it's worth coding tightly. */
		int put_buffer;
		int put_bits;

		/* if size is 0, caller used an invalid Huffman table entry */
		if (size == 0)
		{
			throw new IllegalStateException("JERR_HUFF_MISSING_CODE");
		}

		/* mask off any extra bits in code */
		put_buffer = code & ((1 << size) - 1);

		/* new number of bits in buffer */
		put_bits = size + state.cur.put_bits;

		put_buffer <<= 24 - put_bits;
		/* align incoming bits */

		/* and merge with old buffer contents */
		put_buffer |= state.cur.put_buffer;

		while (put_bits >= 8)
		{
			int c = (put_buffer >>> 16) & 0xFF;

			emit_byte_s(state, c);
			if (c == 0xFF) // need to stuff a zero byte
			{
				emit_byte_s(state, 0);
			}
			put_buffer <<= 8;
			put_bits -= 8;
		}

		state.cur.put_buffer = put_buffer;
		/* update state variables */
		state.cur.put_bits = put_bits;
	}


	/* Emit some bits, unless we are in gather mode */
	private void emit_bits_e(huff_entropy_encoder entropy, int code, int size) throws IOException
	{
		/* This routine is heavily used, so it's worth coding tightly. */
		int put_buffer;
		int put_bits;

		/* if size is 0, caller used an invalid Huffman table entry */
		if (size == 0)
		{
			throw new IllegalStateException("JERR_HUFF_MISSING_CODE");
		}

		if (entropy.gather_statistics)
		{
			return;			/* do nothing if we're only getting stats */
		}

		/* mask off any extra bits in code */
		put_buffer = code & ((1 << size) - 1);

		/* new number of bits in buffer */
		put_bits = size + entropy.saved.put_bits;

		put_buffer <<= 24 - put_bits;
		/* align incoming bits */

		/* and merge with old buffer contents */
		put_buffer |= entropy.saved.put_buffer;

		while (put_bits >= 8)
		{
			int c = (put_buffer >>> 16) & 0xFF;

			emit_byte_e(entropy, c);
			if (c == 0xFF)
			{
				/* need to stuff a zero byte? */
				emit_byte_e(entropy, 0);
			}
			put_buffer <<= 8;
			put_bits -= 8;
		}

		entropy.saved.put_buffer = put_buffer;
		/* update variables */
		entropy.saved.put_bits = put_bits;
	}


	private void flush_bits_s(working_state state) throws IOException
	{
		emit_bits_s(state, 0x7F, 7);
		/* fill any partial byte with ones */
		state.cur.put_buffer = 0;
		/* and reset bit-buffer to empty */
		state.cur.put_bits = 0;
	}


	private void flush_bits_e(huff_entropy_encoder entropy) throws IOException
	{
		if (entropy.gather_statistics) return;

		emit_bits_e(entropy, 0x7F, 7);
		/* fill any partial byte with ones */
		entropy.saved.put_buffer = 0;
		/* and reset bit-buffer to empty */
		entropy.saved.put_bits = 0;
	}


	/*
	 * Emit (or just count) a Huffman symbol.
	 */
	private void emit_dc_symbol(huff_entropy_encoder entropy, int tbl_no, int symbol) throws IOException
	{
		if (entropy.gather_statistics)
		{
			entropy.dc_count_ptrs[tbl_no][symbol]++;
		}
		else
		{
			c_derived_tbl tbl = entropy.dc_derived_tbls[tbl_no];
			emit_bits_e(entropy, tbl.ehufco[symbol], tbl.ehufsi[symbol]);
		}
	}


	private void emit_ac_symbol(huff_entropy_encoder entropy, int tbl_no, int symbol) throws IOException
	{
		if (entropy.gather_statistics)
		{
			entropy.ac_count_ptrs[tbl_no][symbol]++;
		}
		else
		{
			c_derived_tbl tbl = entropy.ac_derived_tbls[tbl_no];
			emit_bits_e(entropy, tbl.ehufco[symbol], tbl.ehufsi[symbol]);
		}
	}


	/*
	 * Emit bits from a correction bit buffer.
	 */
	private void emit_buffered_bits(huff_entropy_encoder entropy, int[] bufstart, int bufoffset, int nbits) throws IOException
	{
		if (entropy.gather_statistics)
		{
			return;			/* no real work */
		}

		while (nbits > 0)
		{
			emit_bits_e(entropy, bufstart[bufoffset], 1);
			bufoffset++;
			nbits--;
		}
	}


	/*
	 * Emit any pending EOBRUN symbol.
	 */
	private void emit_eobrun(huff_entropy_encoder entropy) throws IOException
	{
		int temp, nbits;

		if (entropy.EOBRUN > 0)
		{
			/* if there is any pending EOBRUN */
			temp = entropy.EOBRUN;
			nbits = 0;
			while ((temp >>= 1) != 0)
			{
				nbits++;
			}
			/* safety check: shouldn't happen given limited correction-bit buffer */
			if (nbits > 14)
			{
				throw new IllegalStateException("JERR_HUFF_MISSING_CODE");
			}

			emit_ac_symbol(entropy, entropy.ac_tbl_no, nbits << 4);
			if (nbits != 0)
			{
				emit_bits_e(entropy, entropy.EOBRUN, nbits);
			}

			entropy.EOBRUN = 0;

			/* Emit any buffered correction bits */
			emit_buffered_bits(entropy, entropy.bit_buffer, 0, entropy.BE);
			entropy.BE = 0;
		}
	}


	/*
	 * Emit a restart marker & resynchronize predictions.
	 */
	private boolean emit_restart_s(working_state state, int restart_num) throws IOException
	{
		int ci;

		flush_bits_s(state);

		emit_byte_s(state, 0xFF);
		emit_byte_s(state, RST0 + restart_num);

		/* Re-initialize DC predictions to 0 */
		for (ci = 0; ci < state.cinfo.comps_in_scan; ci++)
		{
			state.cur.last_dc_val[ci] = 0;
		}

		/* The restart counter is not updated until we successfully write the MCU. */
		return true;
	}


	private void emit_restart_e(huff_entropy_encoder entropy, int restart_num) throws IOException
	{
		int ci;

		emit_eobrun(entropy);

		if (!entropy.gather_statistics)
		{
			flush_bits_e(entropy);
			emit_byte_e(entropy, 0xFF);
			emit_byte_e(entropy, RST0 + restart_num);
		}

		if (entropy.cinfo.Ss == 0)
		{
			/* Re-initialize DC predictions to 0 */
			for (ci = 0; ci < entropy.cinfo.comps_in_scan; ci++)
			{
				entropy.saved.last_dc_val[ci] = 0;
			}
		}
		else
		{
			/* Re-initialize all AC-related fields to 0 */
			entropy.EOBRUN = 0;
			entropy.BE = 0;
		}
	}


	/*
	 * MCU encoding for DC initial scan (either spectral selection,
	 * or first pass of successive approximation).
	 */
	private boolean encode_mcu_DC_first(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		int temp, temp2;
		int nbits;
		int blkn, ci, tbl;

//		entropy.next_output_byte = cinfo.next_output_byte;
//		entropy.next_output_byte_offset = cinfo.next_output_byte_offset;
//		entropy.free_in_buffer = cinfo.free_in_buffer;

		/* Emit restart marker if needed */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				emit_restart_e(entropy, entropy.next_restart_num);
			}
		}

		/* Encode the MCU data blocks */
		for (blkn = 0; blkn < aJPEG.blocks_in_MCU; blkn++)
		{
			ci = aJPEG.MCU_membership[blkn];
			tbl = aJPEG.cur_comp_info[ci].getTableDC();

			/* Compute the DC value after the required point transform by Al.
			 * This is simply an arithmetic right shift.
			 */
			temp = aCoefficients[blkn][0] >> aJPEG.Al;

			/* DC differences are figured on the point-transformed values. */
			temp2 = temp - entropy.saved.last_dc_val[ci];
			entropy.saved.last_dc_val[ci] = temp;

			/* Encode the DC coefficient difference per section G.1.2.1 */
			temp = temp2;
			if (temp < 0)
			{
				temp = -temp;
				/* temp is abs value of input */
				/* For a negative input, want temp2 = bitwise complement of abs(input) */
				/* This code assumes we are on a two's complement machine */
				temp2--;
			}

			/* Find the number of bits needed for the magnitude of the coefficient */
			nbits = 0;
			while (temp != 0)
			{
				nbits++;
				temp >>= 1;
			}
			/* Check for out-of-range coefficient values.
			 * Since we're encoding a difference, the range limit is twice as much.
			 */
			if (nbits > MAX_COEF_BITS + 1)
			{
				throw new IllegalStateException("JERR_BAD_DCT_COEF");
			}

			/* Count/emit the Huffman-coded symbol for the number of bits */
			emit_dc_symbol(entropy, tbl, nbits);

			/* Emit that number of bits of the value, if positive, */
			/* or the complement of its magnitude, if negative. */
			if (nbits != 0)
			/* emit_bits rejects calls with size 0 */
			{
				emit_bits_e(entropy, temp2, nbits);
			}
		}

//		cinfo.next_output_byte = entropy.next_output_byte;
//		cinfo.next_output_byte_offset = entropy.next_output_byte_offset;
//		cinfo.free_in_buffer = entropy.free_in_buffer;

		/* Update restart-interval state too */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				entropy.restarts_to_go = aJPEG.mRestartInterval;
				entropy.next_restart_num++;
				entropy.next_restart_num &= 7;
			}
			entropy.restarts_to_go--;
		}

		return true;
	}


	/*
	 * MCU encoding for AC initial scan (either spectral selection,
	 * or first pass of successive approximation).
	 */
	private boolean encode_mcu_AC_first(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		int[] natural_order;
		int[] block;
		int temp, temp2;
		int nbits;
		int r, k;
		int Se, Al;

//		entropy.next_output_byte = cinfo.next_output_byte;
//		entropy.free_in_buffer = cinfo.free_in_buffer;

		/* Emit restart marker if needed */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				emit_restart_e(entropy, entropy.next_restart_num);
			}
		}

		Se = aJPEG.Se;
		Al = aJPEG.Al;
		natural_order = JPEGConstants.NATURAL_ORDER;

		/* Encode the MCU data block */
		block = aCoefficients[0];

		/* Encode the AC coefficients per section G.1.2.2, fig. G.3 */
		r = 0;
		/* r = run length of zeros */

		for (k = aJPEG.Ss; k <= Se; k++)
		{
			if ((temp = block[natural_order[k]]) == 0)
			{
				r++;
				continue;
			}
			/* We must apply the point transform by Al.  For AC coefficients this
			 * is an integer division with rounding towards 0.  To do this portably
			 * in C, we shift after obtaining the absolute value; so the code is
			 * interwoven with finding the abs value (temp) and output bits (temp2).
			 */
			if (temp < 0)
			{
				temp = -temp;
				/* temp is abs value of input */
				temp >>= Al;
				/* apply the point transform */
				/* For a negative coef, want temp2 = bitwise complement of abs(coef) */
				temp2 = ~temp;
			}
			else
			{
				temp >>= Al;
				/* apply the point transform */
				temp2 = temp;
			}
			/* Watch out for case that nonzero coef is zero after point transform */
			if (temp == 0)
			{
				r++;
				continue;
			}

			/* Emit any pending EOBRUN */
			if (entropy.EOBRUN > 0)
			{
				emit_eobrun(entropy);
			}
			/* if run length > 15, must emit special run-length-16 codes (0xF0) */
			while (r > 15)
			{
				emit_ac_symbol(entropy, entropy.ac_tbl_no, 0xF0);
				r -= 16;
			}

			/* Find the number of bits needed for the magnitude of the coefficient */
			nbits = 1;
			/* there must be at least one 1 bit */
			while ((temp >>= 1) != 0)
			{
				nbits++;
			}
			/* Check for out-of-range coefficient values */
			if (nbits > MAX_COEF_BITS)
			{
				throw new IllegalStateException("JERR_BAD_DCT_COEF");
			}

			/* Count/emit Huffman symbol for run length / number of bits */
			emit_ac_symbol(entropy, entropy.ac_tbl_no, (r << 4) + nbits);

			/* Emit that number of bits of the value, if positive, */
			/* or the complement of its magnitude, if negative. */
			emit_bits_e(entropy, temp2, nbits);

			r = 0;
			/* reset zero run length */
		}

		if (r > 0)
		{
			/* If there are trailing zeroes, */
			entropy.EOBRUN++;
			/* count an EOB */
			if (entropy.EOBRUN == 0x7FFF)
			{
				emit_eobrun(entropy);	/* force it out to avoid overflow */
			}
		}

//		cinfo.next_output_byte = entropy.next_output_byte;
//		cinfo.free_in_buffer = entropy.free_in_buffer;

		/* Update restart-interval state too */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				entropy.restarts_to_go = aJPEG.mRestartInterval;
				entropy.next_restart_num++;
				entropy.next_restart_num &= 7;
			}
			entropy.restarts_to_go--;
		}

		return true;
	}


	/*
	 * MCU encoding for DC successive approximation refinement scan.
	 * Note: we assume such scans can be multi-component,
	 * although the spec is not very clear on the point.
	 */
	private boolean encode_mcu_DC_refine(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		int Al, blkn;

//		entropy.next_output_byte = cinfo.next_output_byte;
//		entropy.free_in_buffer = cinfo.free_in_buffer;

		/* Emit restart marker if needed */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				emit_restart_e(entropy, entropy.next_restart_num);
			}
		}

		Al = aJPEG.Al;

		/* Encode the MCU data blocks */
		for (blkn = 0; blkn < aJPEG.blocks_in_MCU; blkn++)
		{
			/* We simply emit the Al'th bit of the DC coefficient value. */
			emit_bits_e(entropy, (aCoefficients[blkn][0] >> Al) & 1, 1);
		}

//		cinfo.next_output_byte = entropy.next_output_byte;
//		cinfo.free_in_buffer = entropy.free_in_buffer;

		/* Update restart-interval state too */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				entropy.restarts_to_go = aJPEG.mRestartInterval;
				entropy.next_restart_num++;
				entropy.next_restart_num &= 7;
			}
			entropy.restarts_to_go--;
		}

		return true;
	}


	/*
	 * MCU encoding for AC successive approximation refinement scan.
	 */
	private boolean encode_mcu_AC_refine(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		int[] natural_order;
		int[] block;
		int temp;
		int r, k;
		int Se, Al;
		int EOB;
		int[] BR_buffer;
		int BR;
		int[] absvalues = new int[DCTSIZE2];

//		entropy.next_output_byte = cinfo.next_output_byte;
//		entropy.free_in_buffer = cinfo.free_in_buffer;

		/* Emit restart marker if needed */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				emit_restart_e(entropy, entropy.next_restart_num);
			}
		}

		Se = aJPEG.Se;
		Al = aJPEG.Al;
		natural_order = JPEGConstants.NATURAL_ORDER;

		/* Encode the MCU data block */
		block = aCoefficients[0];

		/* It is convenient to make a pre-pass to determine the transformed
		 * coefficients' absolute values and the EOB position.
		 */
		EOB = 0;
		for (k = aJPEG.Ss; k <= Se; k++)
		{
			temp = block[natural_order[k]];
			/* We must apply the point transform by Al.  For AC coefficients this
			 * is an integer division with rounding towards 0.  To do this portably
			 * in C, we shift after obtaining the absolute value.
			 */
			if (temp < 0)
			{
				temp = -temp;		/* temp is abs value of input */
			}
			temp >>= Al;
			/* apply the point transform */
			absvalues[k] = temp;
			/* save abs value for main pass */
			if (temp == 1)
			{
				EOB = k;			/* EOB = index of last newly-nonzero coef */
			}
		}

		/* Encode the AC coefficients per section G.1.2.3, fig. G.7 */
		r = 0;
		/* r = run length of zeros */
		BR = 0;
		/* BR = count of buffered bits added now */
		BR_buffer = entropy.bit_buffer;
		/* Append bits to buffer */
		int BR_offset = entropy.BE;

		for (k = aJPEG.Ss; k <= Se; k++)
		{
			if ((temp = absvalues[k]) == 0)
			{
				r++;
				continue;
			}

			/* Emit any required ZRLs, but not if they can be folded into EOB */
			while (r > 15 && k <= EOB)
			{
				/* emit any pending EOBRUN and the BE correction bits */
				emit_eobrun(entropy);
				/* Emit ZRL */
				emit_ac_symbol(entropy, entropy.ac_tbl_no, 0xF0);
				r -= 16;
				/* Emit buffered correction bits that must be associated with ZRL */
				emit_buffered_bits(entropy, BR_buffer, BR_offset, BR);
				BR_offset = 0;
				/* BE bits are gone now */
				BR = 0;
			}

			/* If the coef was previously nonzero, it only needs a correction bit.
			 * NOTE: a straight translation of the spec's figure G.7 would suggest
			 * that we also need to test r > 15.  But if r > 15, we can only get here
			 * if k > EOB, which implies that this coefficient is not 1.
			 */
			if (temp > 1)
			{
				/* The correction bit is the next bit of the absolute value. */
				BR_buffer[BR_offset + BR++] = (temp & 1);
				continue;
			}

			/* Emit any pending EOBRUN and the BE correction bits */
			emit_eobrun(entropy);

			/* Count/emit Huffman symbol for run length / number of bits */
			emit_ac_symbol(entropy, entropy.ac_tbl_no, (r << 4) + 1);

			/* Emit output bit for newly-nonzero coef */
			temp = (block[natural_order[k]] < 0) ? 0 : 1;
			emit_bits_e(entropy, temp, 1);

			/* Emit buffered correction bits that must be associated with this code */
			emit_buffered_bits(entropy, BR_buffer, BR_offset, BR);
			BR_offset = 0;
			/* BE bits are gone now */
			BR = 0;
			r = 0;
			/* reset zero run length */
		}

		if (r > 0 || BR > 0)
		{
			/* If there are trailing zeroes, */
			entropy.EOBRUN++;
			/* count an EOB */
			entropy.BE += BR;
			/* concat my correction bits to older ones */
			/* We force out the EOB if we risk either:
			 * 1. overflow of the EOB counter;
			 * 2. overflow of the correction bit buffer during the next MCU.
			 */
			if (entropy.EOBRUN == 0x7FFF || entropy.BE > (MAX_CORR_BITS - DCTSIZE2 + 1))
			{
				emit_eobrun(entropy);
			}
		}

//		cinfo.next_output_byte = entropy.next_output_byte;
//		cinfo.free_in_buffer = entropy.free_in_buffer;

		/* Update restart-interval state too */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				entropy.restarts_to_go = aJPEG.mRestartInterval;
				entropy.next_restart_num++;
				entropy.next_restart_num &= 7;
			}
			entropy.restarts_to_go--;
		}

		return true;
	}


	/* Encode a single block's worth of coefficients */
	private void encode_one_block(working_state state, int[] block, int last_dc_val, c_derived_tbl dctbl, c_derived_tbl actbl) throws IOException
	{
		int temp, temp2;
		int nbits;
		int r, k;
		int Se = state.cinfo.lim_Se;
		int[] natural_order = JPEGConstants.NATURAL_ORDER;

		/* Encode the DC coefficient difference per section F.1.2.1 */
		temp = temp2 = block[0] - last_dc_val;

		if (temp < 0)
		{
			temp = -temp;
			/* temp is abs value of input */
			/* For a negative input, want temp2 = bitwise complement of abs(input) */
			/* This code assumes we are on a two's complement machine */
			temp2--;
		}

		/* Find the number of bits needed for the magnitude of the coefficient */
		nbits = 0;
		while (temp != 0)
		{
			nbits++;
			temp >>= 1;
		}
		/* Check for out-of-range coefficient values.
		 * Since we're encoding a difference, the range limit is twice as much.
		 */
		if (nbits > MAX_COEF_BITS + 1)
		{
			throw new IllegalStateException("JERR_BAD_DCT_COEF");
		}

		/* Emit the Huffman-coded symbol for the number of bits */
		emit_bits_s(state, dctbl.ehufco[nbits], dctbl.ehufsi[nbits]);

		/* Emit that number of bits of the value, if positive, */
		/* or the complement of its magnitude, if negative. */
		if (nbits != 0)
		/* emit_bits rejects calls with size 0 */
		{
			emit_bits_s(state, temp2, nbits);
		}

		/* Encode the AC coefficients per section F.1.2.2 */
		r = 0;
		/* r = run length of zeros */

		for (k = 1; k <= Se; k++)
		{
			if ((temp2 = block[natural_order[k]]) == 0)
			{
				r++;
			}
			else
			{
				/* if run length > 15, must emit special run-length-16 codes (0xF0) */
				while (r > 15)
				{
					emit_bits_s(state, actbl.ehufco[0xF0], actbl.ehufsi[0xF0]);
					r -= 16;
				}

				temp = temp2;
				if (temp < 0)
				{
					temp = -temp;
					/* temp is abs value of input */
					/* This code assumes we are on a two's complement machine */
					temp2--;
				}

				/* Find the number of bits needed for the magnitude of the coefficient */
				nbits = 1;
				/* there must be at least one 1 bit */
				while ((temp >>= 1) != 0)
				{
					nbits++;
				}
				/* Check for out-of-range coefficient values */
				if (nbits > MAX_COEF_BITS)
				{
					throw new IllegalStateException("JERR_BAD_DCT_COEF");
				}

				/* Emit Huffman symbol for run length / number of bits */
				temp = (r << 4) + nbits;
				emit_bits_s(state, actbl.ehufco[temp], actbl.ehufsi[temp]);

				/* Emit that number of bits of the value, if positive, */
				/* or the complement of its magnitude, if negative. */
				emit_bits_s(state, temp2, nbits);

				r = 0;
			}
		}

		/* If the last coef(s) were zero, emit an end-of-block code */
		if (r > 0)
		{
			emit_bits_s(state, actbl.ehufco[0], actbl.ehufsi[0]);
		}
	}


	/*
	 * Encode and output one MCU's worth of Huffman-compressed coefficients.
	 */
	private boolean encode_mcu_huff(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		working_state state = entropy.working_state;
		int blkn, ci;
		ComponentInfo compptr;

		/* Load up working state */
//		state.next_output_byte = aJPEG.next_output_byte;
//		state.next_output_byte_offset = aJPEG.next_output_byte_offset;
//		state.free_in_buffer = aJPEG.free_in_buffer;
		state.cur = entropy.saved;
		state.cinfo = aJPEG;

		/* Emit restart marker if needed */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				emit_restart_s(state, entropy.next_restart_num);
			}
		}

		/* Encode the MCU data blocks */
		for (blkn = 0; blkn < aJPEG.blocks_in_MCU; blkn++)
		{
			ci = aJPEG.MCU_membership[blkn];
			compptr = aJPEG.cur_comp_info[ci];
			encode_one_block(state, aCoefficients[blkn], state.cur.last_dc_val[ci], entropy.dc_derived_tbls[compptr.getTableDC()], entropy.ac_derived_tbls[compptr.getTableAC()]);
			/* Update last_dc_val */
			state.cur.last_dc_val[ci] = aCoefficients[blkn][0];
		}

		/* Completed MCU, so update state */
//		cinfo.next_output_byte = state.next_output_byte;
//		cinfo.free_in_buffer = state.free_in_buffer;
		entropy.saved = state.cur;

		/* Update restart-interval state too */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				entropy.restarts_to_go = aJPEG.mRestartInterval;
				entropy.next_restart_num++;
				entropy.next_restart_num &= 7;
			}
			entropy.restarts_to_go--;
		}

		return true;
	}


	/*
	 * Finish up at the end of a Huffman-compressed scan.
	 */
	@Override
	public void finish_pass(JPEG aJPEG, boolean gather_statistics) throws IOException
	{
		if (gather_statistics)
		{
			finish_pass_gather(aJPEG);
		}
		else
		{
			finish_pass_huff(aJPEG);
		}
	}


	private void finish_pass_huff(JPEG aJPEG) throws IOException
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		working_state state = new working_state();

		if (aJPEG.mProgressive)
		{
//			entropy.next_output_byte[entropy.next_output_byte_offset] = cinfo.next_output_byte[cinfo.next_output_byte_offset];
//			entropy.free_in_buffer = cinfo.free_in_buffer;

			/* Flush out any buffered data */
			emit_eobrun(entropy);
			flush_bits_e(entropy);

			dump_buffer_e(entropy);

//			cinfo.next_output_byte[cinfo.next_output_byte_offset] = entropy.next_output_byte[entropy.next_output_byte_offset];
//			cinfo.free_in_buffer = entropy.free_in_buffer;
		}
		else
		{
			/* Load up working state ... flush_bits needs it */
//			state.next_output_byte = cinfo.next_output_byte;
//			state.free_in_buffer = cinfo.free_in_buffer;
			state.cur = entropy.saved;
			state.cinfo = aJPEG;

			/* Flush out the last data */
			flush_bits_s(entropy.working_state);

			dump_buffer_s(entropy.working_state);

			/* Update state */
//			cinfo.next_output_byte = state.next_output_byte;
//			cinfo.free_in_buffer = state.free_in_buffer;
			entropy.saved = state.cur;
		}
	}


	/*
	 * Huffman coding optimization.
	 *
	 * We first scan the supplied data and count the number of uses of each symbol
	 * that is to be Huffman-coded. (This process MUST agree with the code above.)
	 * Then we build a Huffman coding tree for the observed counts.
	 * Symbols which are not needed at all for the particular image are not
	 * assigned any code, which saves space in the DHT marker as well as in
	 * the compressed data.
	 */
	/* Process a single block's worth of coefficients */
	private void htest_one_block(JPEG aJPEG, int[] block, int last_dc_val, int[] dc_counts, int[] ac_counts)
	{
		int temp;
		int nbits;
		int r, k;
		int Se = aJPEG.lim_Se;
		int[] natural_order = JPEGConstants.NATURAL_ORDER;

		/* Encode the DC coefficient difference per section F.1.2.1 */
		temp = block[0] - last_dc_val;
		if (temp < 0)
		{
			temp = -temp;
		}

		/* Find the number of bits needed for the magnitude of the coefficient */
		nbits = 0;
		while (temp != 0)
		{
			nbits++;
			temp >>= 1;
		}
		/* Check for out-of-range coefficient values.
		 * Since we're encoding a difference, the range limit is twice as much.
		 */
		if (nbits > MAX_COEF_BITS + 1)
		{
			throw new IllegalStateException("JERR_BAD_DCT_COEF");
		}

		/* Count the Huffman symbol for the number of bits */
		dc_counts[nbits]++;

		/* Encode the AC coefficients per section F.1.2.2 */
		r = 0;
		/* r = run length of zeros */

		for (k = 1; k <= Se; k++)
		{
			if ((temp = block[natural_order[k]]) == 0)
			{
				r++;
			}
			else
			{
				/* if run length > 15, must emit special run-length-16 codes (0xF0) */
				while (r > 15)
				{
					ac_counts[0xF0]++;
					r -= 16;
				}

				/* Find the number of bits needed for the magnitude of the coefficient */
				if (temp < 0)
				{
					temp = -temp;
				}

				/* Find the number of bits needed for the magnitude of the coefficient */
				nbits = 1;
				/* there must be at least one 1 bit */
				while ((temp >>= 1) != 0)
				{
					nbits++;
				}
				/* Check for out-of-range coefficient values */
				if (nbits > MAX_COEF_BITS)
				{
					throw new IllegalStateException("JERR_BAD_DCT_COEF");
				}

				/* Count Huffman symbol for run length / number of bits */
				ac_counts[(r << 4) + nbits]++;

				r = 0;
			}
		}

		/* If the last coef(s) were zero, emit an end-of-block code */
		if (r > 0)
		{
			ac_counts[0]++;
		}
	}


	/*
	 * Trial-encode one MCU's worth of Huffman-compressed coefficients.
	 * No data is actually output, so no suspension return is possible.
	 */
	private boolean encode_mcu_gather(JPEG aJPEG, int[][] aCoefficients)
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		int blkn, ci;
		ComponentInfo compptr;

		/* Take care of restart intervals if needed */
		if (aJPEG.mRestartInterval != 0)
		{
			if (entropy.restarts_to_go == 0)
			{
				/* Re-initialize DC predictions to 0 */
				for (ci = 0; ci < aJPEG.comps_in_scan; ci++)
				{
					entropy.saved.last_dc_val[ci] = 0;
				}
				/* Update restart state */
				entropy.restarts_to_go = aJPEG.mRestartInterval;
			}
			entropy.restarts_to_go--;
		}

		for (blkn = 0; blkn < aJPEG.blocks_in_MCU; blkn++)
		{
			ci = aJPEG.MCU_membership[blkn];
			compptr = aJPEG.cur_comp_info[ci];
			htest_one_block(aJPEG, aCoefficients[blkn], entropy.saved.last_dc_val[ci], entropy.dc_count_ptrs[compptr.getTableDC()], entropy.ac_count_ptrs[compptr.getTableAC()]);
			entropy.saved.last_dc_val[ci] = aCoefficients[blkn][0];
		}

		return true;
	}


	/*
	 * Generate the best Huffman code table for the given counts, fill htbl.
	 *
	 * The JPEG standard requires that no symbol be assigned a codeword of all
	 * one bits (so that padding bits added at the end of a compressed segment
	 * can't look like a valid code).  Because of the canonical ordering of
	 * codewords, this just means that there must be an unused slot in the
	 * longest codeword length category.  Section K.2 of the JPEG spec suggests
	 * reserving such a slot by pretending that symbol 256 is a valid symbol
	 * with count 1.  In theory that's not optimal; giving it count zero but
	 * including it in the symbol set anyway should give a better Huffman code.
	 * But the theoretically better code actually seems to come out worse in
	 * practice, because it produces more all-ones bytes (which incur stuffed
	 * zero bytes in the final file).  In any case the difference is tiny.
	 *
	 * The JPEG standard requires Huffman codes to be no more than 16 bits long.
	 * If some symbols have a very small but nonzero probability, the Huffman tree
	 * must be adjusted to meet the code length restriction.  We currently use
	 * the adjustment method suggested in JPEG section K.2.  This method is *not*
	 * optimal; it may not choose the best possible limited-length code.  But
	 * typically only very-low-frequency symbols will be given less-than-optimal
	 * lengths, so the code is almost optimal.  Experimental comparisons against
	 * an optimal limited-length-code algorithm indicate that the difference is
	 * microscopic --- usually less than a hundredth of a percent of total size.
	 * So the extra complexity of an optimal algorithm doesn't seem worthwhile.
	 */
	private HuffmanTable jpeg_gen_optimal_table(JPEG aJPEG, int[] freq, int aType, int aIndex)
	{
		int MAX_CLEN = 32;
		/* assumed maximum initial code length */
		int[] bits = new int[MAX_CLEN + 1];
		/* bits[k] = # of symbols with code length k */
		int[] codesize = new int[257];
		/* codesize[k] = code length of symbol k */
		int[] others = new int[257];
		/* next symbol in current branch of tree */
		int c1, c2;
		int p, i, j;
		long v;

		/* This algorithm is explained in section K.2 of the JPEG standard */
		for (i = 0; i < 257; i++)
		{
			others[i] = -1;		/* init links to empty */
		}

		freq[256] = 1;
		/* make sure 256 has a nonzero count */
		/* Including the pseudo-symbol 256 in the Huffman procedure guarantees
		  * that no real symbol is given code-value of all ones, because 256
		  * will be placed last in the largest codeword category.
		 */

		/* Huffman's basic algorithm to assign optimal code lengths to symbols */

		for (;;)
		{
			/* Find the smallest nonzero frequency, set c1 = its symbol */
			/* In case of ties, take the larger symbol number */
			c1 = -1;
			v = 1000000000L;
			for (i = 0; i <= 256; i++)
			{
				if (freq[i] != 0 && freq[i] <= v)
				{
					v = freq[i];
					c1 = i;
				}
			}

			/* Find the next smallest nonzero frequency, set c2 = its symbol */
			/* In case of ties, take the larger symbol number */
			c2 = -1;
			v = 1000000000L;
			for (i = 0; i <= 256; i++)
			{
				if (freq[i] != 0 && freq[i] <= v && i != c1)
				{
					v = freq[i];
					c2 = i;
				}
			}

			/* Done if we've merged everything into one frequency */
			if (c2 < 0)
			{
				break;
			}

			/* Else merge the two counts/trees */
			freq[c1] += freq[c2];
			freq[c2] = 0;

			/* Increment the codesize of everything in c1's tree branch */
			codesize[c1]++;
			while (others[c1] >= 0)
			{
				c1 = others[c1];
				codesize[c1]++;
			}

			others[c1] = c2;
			/* chain c2 onto c1's tree branch */

			/* Increment the codesize of everything in c2's tree branch */
			codesize[c2]++;
			while (others[c2] >= 0)
			{
				c2 = others[c2];
				codesize[c2]++;
			}
		}

		/* Now count the number of symbols of each code length */
		for (i = 0; i <= 256; i++)
		{
			if (codesize[i] != 0)
			{
				/* The JPEG standard seems to think that this can't happen, */
				/* but I'm paranoid... */
				if (codesize[i] > MAX_CLEN)
				{
					throw new IllegalStateException("JERR_HUFF_CLEN_OVERFLOW");
				}

				bits[codesize[i]]++;
			}
		}

		/* JPEG doesn't allow symbols with code lengths over 16 bits, so if the pure
		 * Huffman procedure assigned any such lengths, we must adjust the coding.
		 * Here is what the JPEG spec says about how this next bit works:
		 * Since symbols are paired for the longest Huffman code, the symbols are
		 * removed from this length category two at a time.  The prefix for the pair
		 * (which is one bit shorter) is allocated to one of the pair; then,
		 * skipping the BITS entry for that prefix length, a code word from the next
		 * shortest nonzero BITS entry is converted into a prefix for two code words
		 * one bit longer.
		 */
		for (i = MAX_CLEN; i > 16; i--)
		{
			while (bits[i] > 0)
			{
				j = i - 2;
				/* find length of new prefix to be used */
				while (bits[j] == 0)
				{
					j--;
				}

				bits[i] -= 2;
				/* remove two symbols */
				bits[i - 1]++;
				/* one goes in this length */
				bits[j + 1] += 2;
				/* two new symbols in this length */
				bits[j]--;
				/* symbol of this length is now a prefix */
			}
		}

		/* Remove the count for the pseudo-symbol 256 from the largest codelength */
		while (bits[i] == 0)
		/* find largest codelength still in use */
		{
			i--;
		}
		bits[i]--;

		int[] huffval = new int[257];

		/* Return a list of the symbols sorted by code length */
		/* It's not real clear to me why we don't need to consider the codelength
		 * changes made above, but the JPEG spec seems to think this works.
		 */
		p = 0;
		for (i = 1; i <= MAX_CLEN; i++)
		{
			for (j = 0; j <= 255; j++)
			{
				if (codesize[j] == i)
				{
					huffval[p] = j;
					p++;
				}
			}
		}

		return new HuffmanTable(aType, aIndex, bits, huffval);
	}


	/*
	 * Finish up a statistics-gathering pass and create the new Huffman tables.
	 */
	private void finish_pass_gather(JPEG aJPEG) throws IOException
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		int ci, tbl;
		ComponentInfo compptr;
		boolean[] did_dc = new boolean[NUM_HUFF_TBLS];
		boolean[] did_ac = new boolean[NUM_HUFF_TBLS];

		/* It's important not to apply jpeg_gen_optimal_table more than once
		 * per table, because it clobbers the input frequency counts!
		 */
		if (aJPEG.mProgressive)	/* Flush out buffered data (all we care about is counting the EOB symbol) */
		{
			emit_eobrun(entropy);
		}

//  MEMZERO(did_dc, SIZEOF(did_dc));
//  MEMZERO(did_ac, SIZEOF(did_ac));

		for (ci = 0; ci < aJPEG.comps_in_scan; ci++)
		{
			compptr = aJPEG.cur_comp_info[ci];
			/* DC needs no table for refinement scan */
			if (aJPEG.Ss == 0 && aJPEG.Ah == 0)
			{
				tbl = compptr.getTableDC();
				if (!did_dc[tbl])
				{
					aJPEG.dc_huff_tbl_ptrs[tbl] = jpeg_gen_optimal_table(aJPEG, entropy.dc_count_ptrs[tbl], HuffmanTable.TYPE_DC, tbl);
					did_dc[tbl] = true;
				}
			}
			/* AC needs no table when not present */
			if (aJPEG.Se != 0)
			{
				tbl = compptr.getTableAC();
				if (!did_ac[tbl])
				{
					aJPEG.ac_huff_tbl_ptrs[tbl] = jpeg_gen_optimal_table(aJPEG, entropy.ac_count_ptrs[tbl], HuffmanTable.TYPE_AC, tbl);
					did_ac[tbl] = true;
				}
			}
		}
	}


	/*
	 * Initialize for a Huffman-compressed scan.
	 * If gather_statistics is TRUE, we do not output anything during the scan,
	 * just count the Huffman symbols used and generate Huffman code tables.
	 */
	@Override
	public void start_pass(JPEG aJPEG, boolean gather_statistics)
	{
		huff_entropy_encoder entropy = (huff_entropy_encoder)aJPEG.entropy;
		int ci, tbl;
		ComponentInfo compptr;

		entropy.cinfo = aJPEG;

		if (aJPEG.mProgressive)
		{
			entropy.gather_statistics = gather_statistics;

			/* We assume jcmaster.c already validated the scan parameters. */

			/* Select execution routine */
			if (aJPEG.Ah == 0)
			{
				if (aJPEG.Ss == 0)
				{
					entropy.encode_mcu = x_encode_mcu_DC_first;
				}
				else
				{
					entropy.encode_mcu = x_encode_mcu_AC_first;
				}
			}
			else
			{
				if (aJPEG.Ss == 0)
				{
					entropy.encode_mcu = x_encode_mcu_DC_refine;
				}
				else
				{
					entropy.encode_mcu = x_encode_mcu_AC_refine;
					/* AC refinement needs a correction bit buffer */
					if (entropy.bit_buffer == null)
					{
						entropy.bit_buffer = new int[MAX_CORR_BITS];
					}
				}
			}

			/* Initialize AC stuff */
			entropy.ac_tbl_no = aJPEG.cur_comp_info[0].getTableAC();
			entropy.EOBRUN = 0;
			entropy.BE = 0;
		}
		else
		{
			entropy.encode_mcu = x_encode_mcu;
		}

		for (ci = 0; ci < aJPEG.comps_in_scan; ci++)
		{
			compptr = aJPEG.cur_comp_info[ci];
			/* DC needs no table for refinement scan */
			if (aJPEG.Ss == 0 && aJPEG.Ah == 0)
			{
				tbl = compptr.getTableDC();
				if (gather_statistics)
				{
					/* Check for invalid table index (make_c_derived_tbl does this in the other path) */
					if (tbl < 0 || tbl >= NUM_HUFF_TBLS)
					{
						throw new IllegalStateException("JERR_NO_HUFF_TABLE" + tbl);
					}
					/* Allocate and zero the statistics tables */
					/* Note that jpeg_gen_optimal_table expects 257 entries in each table! */
					if (entropy.dc_count_ptrs[tbl] == null)
					{
						entropy.dc_count_ptrs[tbl] = new int[257];
					}
					Arrays.fill(entropy.dc_count_ptrs[tbl], 0);
				}
				else
				{
					/* Compute derived values for Huffman tables */
					/* We may do this more than once for a table, but it's not expensive */
					entropy.dc_derived_tbls[tbl] = jpeg_make_c_derived_tbl(aJPEG, true, tbl, entropy.dc_derived_tbls[tbl]);
				}
				/* Initialize DC predictions to 0 */
				entropy.saved.last_dc_val[ci] = 0;
			}
			/* AC needs no table when not present */
			if (aJPEG.Se != 0)
			{
				tbl = compptr.getTableAC();
				if (gather_statistics)
				{
					if (tbl < 0 || tbl >= NUM_HUFF_TBLS)
					{
						throw new IllegalStateException("JERR_NO_HUFF_TABLE" + tbl);
					}
					if (entropy.ac_count_ptrs[tbl] == null)
					{
						entropy.ac_count_ptrs[tbl] = new int[257];
					}
					Arrays.fill(entropy.ac_count_ptrs[tbl], 0);
				}
				else
				{
					entropy.ac_derived_tbls[tbl] = jpeg_make_c_derived_tbl(aJPEG, false, tbl, entropy.ac_derived_tbls[tbl]);
				}
			}
		}

		/* Initialize bit buffer to empty */
		entropy.saved.put_buffer = 0;
		entropy.saved.put_bits = 0;

		/* Initialize restart stuff */
		entropy.restarts_to_go = aJPEG.mRestartInterval;
		entropy.next_restart_num = 0;
	}


	/*
	 * Module initialization routine for Huffman entropy encoding.
	 */
	@Override
	public void jinit_encoder(JPEG aJPEG)
	{
		huff_entropy_encoder entropy = new huff_entropy_encoder();
		int i;

		aJPEG.entropy = entropy;

		/* Mark tables unallocated */
		for (i = 0; i < NUM_HUFF_TBLS; i++)
		{
			entropy.dc_derived_tbls[i] = entropy.ac_derived_tbls[i] = null;
			entropy.dc_count_ptrs[i] = entropy.ac_count_ptrs[i] = null;
		}

		if (aJPEG.mProgressive)
		{
			entropy.bit_buffer = null;	/* needed only in AC refinement scan */
		}
	}


	@Override
	public boolean encode_mcu(JPEG aJPEG, int[][] aCoefficients, boolean gather_statistics) throws IOException
	{
		switch (aJPEG.entropy.encode_mcu)
		{
			case x_encode_mcu_DC_first:
				return encode_mcu_DC_first(aJPEG, aCoefficients);
			case x_encode_mcu_AC_first:
				return encode_mcu_AC_first(aJPEG, aCoefficients);
			case x_encode_mcu_DC_refine:
				return encode_mcu_DC_refine(aJPEG, aCoefficients);
			case x_encode_mcu_AC_refine:
				return encode_mcu_AC_refine(aJPEG, aCoefficients);
		}

		if (gather_statistics)
		{
			return encode_mcu_gather(aJPEG, aCoefficients);
		}

		return encode_mcu_huff(aJPEG, aCoefficients);
	}



	/* Set up the standard Huffman tables (cf. JPEG standard section K.3) */
	/* IMPORTANT: these are only valid for 8-bit data precision! */
	public static void setupStandardHuffmanTables(JPEG aJPEG)
	{
		int[] bits_dc_luminance = { /* 0-base */ 0, 0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 };
		int[] val_dc_luminance = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

		int[] bits_dc_chrominance = { /* 0-base */ 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
		int[] val_dc_chrominance = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

		int[] bits_ac_luminance = { /* 0-base */ 0, 0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 0x7d };
		int[] val_ac_luminance =
		  { 0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12,
			0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
			0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08,
			0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
			0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16,
			0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
			0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
			0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
			0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
			0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
			0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
			0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
			0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
			0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
			0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
			0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
			0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4,
			0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
			0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea,
			0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
			0xf9, 0xfa };

		int[] bits_ac_chrominance = { /* 0-base */ 0, 0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 0x77 };
		int[] val_ac_chrominance =
		  { 0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
			0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
			0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91,
			0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
			0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34,
			0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
			0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38,
			0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
			0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
			0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
			0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
			0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
			0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96,
			0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
			0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4,
			0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
			0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2,
			0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
			0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9,
			0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
			0xf9, 0xfa };

		aJPEG.dc_huff_tbl_ptrs = new HuffmanTable[2];
		aJPEG.ac_huff_tbl_ptrs = new HuffmanTable[2];

		aJPEG.dc_huff_tbl_ptrs[0] = new HuffmanTable(HuffmanTable.TYPE_DC, 0, bits_dc_luminance, val_dc_luminance);
		aJPEG.dc_huff_tbl_ptrs[1] = new HuffmanTable(HuffmanTable.TYPE_DC, 1, bits_dc_chrominance, val_dc_chrominance);
		aJPEG.ac_huff_tbl_ptrs[0] = new HuffmanTable(HuffmanTable.TYPE_AC, 0, bits_ac_luminance, val_ac_luminance);
		aJPEG.ac_huff_tbl_ptrs[1] = new HuffmanTable(HuffmanTable.TYPE_AC, 1, bits_ac_chrominance, val_ac_chrominance);
  }
}
