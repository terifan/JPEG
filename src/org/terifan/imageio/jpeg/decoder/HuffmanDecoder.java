package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import java.util.Arrays;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.DHTSegment.HuffmanTable;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import static org.terifan.imageio.jpeg.decoder.JPEGImageReader.MAX_CHANNELS;


public class HuffmanDecoder extends Decoder
{
	private int[] mPreviousDCValue;


	public HuffmanDecoder(BitInputStream aBitStream)
	{
		super(aBitStream);

		mPreviousDCValue = new int[MAX_CHANNELS];
	}


	@Override
	void initialize(JPEG aJPEG)
	{
		ArithEntropyState entropy = new ArithEntropyState();

		aJPEG.entropy = entropy;
	}


	@Override
	void startPass(JPEG aJPEG) throws IOException
	{
		aJPEG.entropy.restarts_to_go = aJPEG.restart_interval;

		mBitStream.align();

		EOBRUN = 0;

		if (VERBOSE)
		{
			if (aJPEG.mProgressive)
			{
				if (aJPEG.Ah == 0)
				{
					if (aJPEG.Ss == 0)
					{
						System.out.println("  decode_mcu_DC_first, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al);
					}
					else
					{
						System.out.println("  decode_mcu_AC_first, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al);
					}
				}
				else
				{
					if (aJPEG.Ss == 0)
					{
						System.out.println("  decode_mcu_DC_refine, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al);
					}
					else
					{
						System.out.println("  decode_mcu_AC_refine, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al);
					}
				}
			}
		}
	}


	@Override
	void finishPass(JPEG aJPEG)
	{
	}


	@Override
	boolean decodeMCU(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		if (aJPEG.restart_interval != 0)
		{
			if (aJPEG.entropy.restarts_to_go == 0)
			{
				for (int ci = 0; ci < aJPEG.comps_in_scan; ci++)
				{
					mPreviousDCValue[aJPEG.MCU_membership[ci]] = 0;
				}

				EOBRUN = 0;

				mBitStream.align();

				int restartMarker = mBitStream.readInt16();
				if (restartMarker != 0xFFD0 + aJPEG.restartMarkerIndex)
				{
					throw new IOException("Error reading JPEG stream; Expected restart marker " + Integer.toHexString(0xFFD0 + aJPEG.restartMarkerIndex));
				}

				aJPEG.restartMarkerIndex = (aJPEG.restartMarkerIndex + 1) & 7;
				aJPEG.entropy.restarts_to_go = aJPEG.restart_interval;
			}

			aJPEG.entropy.restarts_to_go--;
		}

		if (aJPEG.mProgressive)
		{
			if (aJPEG.Ah == 0)
			{
				if (aJPEG.Ss == 0)
				{
					return decode_mcu_DC_first(aJPEG, aCoefficients);
				}
				return decode_mcu_AC_first(aJPEG, aCoefficients);
			}

			if (aJPEG.Ss == 0)
			{
				return decode_mcu_DC_refine(aJPEG, aCoefficients);
			}

			return decode_mcu_AC_refine(aJPEG, aCoefficients);
		}

		return decodeImpl(aJPEG, aCoefficients);
	}


	private boolean decode_mcu_DC_first(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		for (int blockIndex = 0; blockIndex < aJPEG.blocks_in_MCU; blockIndex++)
		{
			int ci = aJPEG.MCU_membership[blockIndex];
			ComponentInfo comp = aJPEG.cur_comp_info[ci];

			Arrays.fill(aCoefficients[blockIndex], 0);

			HuffmanTable dcTable = aJPEG.mHuffmanTables[comp.getTableDC()][DHTSegment.TYPE_DC];

			int value = dcTable.decodeSymbol(mBitStream);

			if (value == -1)
			{
				return false;
			}

			if (value > 0)
			{
				mPreviousDCValue[ci] += dcTable.readCoefficient(mBitStream, value) << aJPEG.Al;
			}

			aCoefficients[blockIndex][0] = mPreviousDCValue[ci];
		}

		return true;
	}

	int EOBRUN;

	private boolean decode_mcu_AC_first(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		if (EOBRUN > 0)
		{
			EOBRUN--;
		}
		else
		{
			int ci = aJPEG.MCU_membership[0];
			int[] coefficients = aCoefficients[0];

			ComponentInfo comp = aJPEG.cur_comp_info[ci];

			HuffmanTable acTable = aJPEG.mHuffmanTables[comp.getTableAC()][DHTSegment.TYPE_AC];

			for (int k = aJPEG.Ss; k <= aJPEG.Se; k++)
			{
				int s = acTable.decodeSymbol(mBitStream);

				if (s == -1)
				{
					return false;
				}

				int r = s >> 4;
				s &= 15;

				if (s != 0)
				{
					k += r;

					coefficients[NATURAL_ORDER[k]] = acTable.readCoefficient(mBitStream, s) << aJPEG.Al;
				}
				else
				{
					if (r != 15)
					{
						if (r != 0)
						{
							EOBRUN = 1 << r;
							EOBRUN += mBitStream.readBits(r) - 1;
						}
						break;
					}
					k += 15;
				}
			}
		}

		return true;
	}


	private boolean decode_mcu_DC_refine(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		for (int blockIndex = 0; blockIndex < aJPEG.blocks_in_MCU; blockIndex++)
		{
			if (mBitStream.readBits(1) != 0)
			{
				aCoefficients[blockIndex][NATURAL_ORDER[0]] |= 1 << aJPEG.Al;
			}
		}

		return true;
	}


	private boolean decode_mcu_AC_refine(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		int p1 = 1 << aJPEG.Al; // 1 in the bit position being coded
		int m1 = (-1) << aJPEG.Al; // -1 in the bit position being coded

		int ci = aJPEG.MCU_membership[0];
		ComponentInfo comp = aJPEG.cur_comp_info[ci];

		HuffmanTable acTable = aJPEG.mHuffmanTables[comp.getTableAC()][DHTSegment.TYPE_AC];

		int k = aJPEG.Ss;
		int[] coefficients = aCoefficients[0];

		if (EOBRUN == 0)
		{
			do
			{
				int s = acTable.decodeSymbol(mBitStream);
				int r = s >> 4;
				s &= 15;

				if (s != 0)
				{
					if (s != 1) // size of new coef should always be 1
					{
						throw new IllegalArgumentException();
					}
					if (mBitStream.readBits(1) != 0)
					{
						s = p1;		// newly nonzero coef is positive
					}
					else
					{
						s = m1;		// newly nonzero coef is negative
					}
				}
				else
				{
					if (r != 15)
					{
						EOBRUN = 1 << r; // EOBr, run length is 2^r + appended bits
						if (r != 0)
						{
							r = mBitStream.readBits(r);
							EOBRUN += r;
						}
						break;
						// rest of block is handled by EOB logic
					}
					// note s = 0 for processing ZRL
				}

				// Advance over already-nonzero coefs and r still-zero coefs, appending correction bits to the nonzeroes.  A correction bit is 1 if the absolute value of the coefficient must be increased.
				do
				{
					int thiscoef = coefficients[NATURAL_ORDER[k]];
					if (thiscoef != 0)
					{
						if (mBitStream.readBits(1) != 0)
						{
							if ((thiscoef & p1) == 0)
							{
								if (thiscoef >= 0) // do nothing if already set it
								{
									coefficients[NATURAL_ORDER[k]] += p1;
								}
								else
								{
									coefficients[NATURAL_ORDER[k]] += m1;
								}
							}
						}
					}
					else
					{
						if (--r < 0)
						{
							break; // reached target zero coefficient
						}
					}
					k++;
				}
				while (k <= aJPEG.Se);
				if (s != 0)
				{
					coefficients[NATURAL_ORDER[k]] = s; // Output newly nonzero coefficient
				}
				k++;
			}
			while (k <= aJPEG.Se);
		}

		if (EOBRUN != 0)
		{
			// Scan any remaining coefficient positions after the end-of-band (the last newly nonzero coefficient, if any). Append a correction bit to each already-nonzero coefficient.  A correction bit is 1 if the absolute value of the coefficient must be increased.
			do
			{
				int thiscoef = coefficients[NATURAL_ORDER[k]];
				if (thiscoef != 0)
				{
					if (mBitStream.readBits(1) != 0)
					{
						if ((thiscoef & p1) == 0)
						{
							if (thiscoef >= 0) // do nothing if already changed it
							{
								coefficients[NATURAL_ORDER[k]] += p1;
							}
							else
							{
								coefficients[NATURAL_ORDER[k]] += m1;
							}
						}
					}
				}
				k++;
			}
			while (k <= aJPEG.Se);

			EOBRUN--; // Count one block completed in EOB run
		}

		return true;
	}


	private boolean decodeImpl(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		for (int blockIndex = 0; blockIndex < aJPEG.blocks_in_MCU; blockIndex++)
		{
			int ci = aJPEG.MCU_membership[blockIndex];
			ComponentInfo comp = aJPEG.cur_comp_info[ci];

			HuffmanTable dcTable = aJPEG.mHuffmanTables[comp.getTableDC()][DHTSegment.TYPE_DC];
			HuffmanTable acTable = aJPEG.mHuffmanTables[comp.getTableAC()][DHTSegment.TYPE_AC];

			Arrays.fill(aCoefficients[blockIndex], 0);

			int value = dcTable.decodeSymbol(mBitStream);

			if (value == -1)
			{
				return false;
			}

			if (value > 0)
			{
				mPreviousDCValue[ci] += dcTable.readCoefficient(mBitStream, value) << aJPEG.Al;
			}

			aCoefficients[blockIndex][NATURAL_ORDER[0]] = mPreviousDCValue[ci];

			if (acTable == null) continue;

			for (int offset = 1; offset < 64; offset++)
			{
				value = acTable.decodeSymbol(mBitStream);

				if (value == -1)
				{
					return false;
				}

				int zeroCount = value >> 4;
				int codeLength = value & 15;

				offset += zeroCount;

				if (codeLength > 0)
				{
					aCoefficients[blockIndex][NATURAL_ORDER[offset]] = acTable.readCoefficient(mBitStream, codeLength) << aJPEG.Al;
				}
				else if (zeroCount == 0)
				{
					break; // EOB found.
				}
				else if (zeroCount != 15)
				{
					throw new IOException("Error reading JPEG stream; ZeroCount must be 0 or 15 when codeLength equals 0.");
				}
			}
		}

		return true;
	}
}