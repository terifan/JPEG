package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.JPEGEntropyState;
import java.io.IOException;
import java.util.Arrays;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.HuffmanTable;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;


public class HuffmanDecoder extends Decoder
{
	private boolean mProgressive;
	private BitInputStream mBitStream;
	private int mEOBRun;
	private JPEGEntropyState aJPEG_entropy;


	public HuffmanDecoder()
	{
	}


	@Override
	void initialize(JPEG aJPEG, BitInputStream aBitStream)
	{
		mBitStream = aBitStream;
		mProgressive = aJPEG.mSOFSegment.getCompressionType().isProgressive();

		aJPEG_entropy = new JPEGEntropyState();
	}


	@Override
	void startPass(JPEG aJPEG) throws IOException
	{
		aJPEG_entropy.restarts_to_go = aJPEG.mRestartInterval;

		for (int ci = 0; ci < aJPEG.mScanBlockCount; ci++)
		{
			aJPEG_entropy.last_dc_val[ci] = 0;
		}

		mBitStream.align();
		mEOBRun = 0;
	}


	@Override
	void finishPass(JPEG aJPEG) throws IOException
	{
		mBitStream.align();
	}


	@Override
	boolean decodeMCU(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		if (aJPEG.mRestartInterval != 0)
		{
			if (aJPEG_entropy.restarts_to_go == 0)
			{
				for (int ci = 0; ci < aJPEG.mScanBlockCount; ci++)
				{
					aJPEG_entropy.last_dc_val[ci] = 0;
				}

				mBitStream.align();
				mEOBRun = 0;

				int restartMarker = mBitStream.readInt16();
				if (restartMarker != 0xFFD0 + aJPEG.mRestartMarkerIndex)
				{
					throw new IOException("Error reading JPEG stream; Expected restart marker " + Integer.toHexString(0xFFD0 + aJPEG.mRestartMarkerIndex));
				}

				aJPEG.mRestartMarkerIndex = (aJPEG.mRestartMarkerIndex + 1) & 7;
				aJPEG_entropy.restarts_to_go = aJPEG.mRestartInterval;
			}

			aJPEG_entropy.restarts_to_go--;
		}

		if (mProgressive)
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
		JPEGEntropyState entropy = aJPEG_entropy;

		for (int blockIndex = 0; blockIndex < aJPEG.mMCUBlockCount; blockIndex++)
		{
			int ci = aJPEG.mMCUComponentIndices[blockIndex];
			ComponentInfo comp = aJPEG.mComponentInfo[ci];

			HuffmanTable dcTable = aJPEG.mHuffmanTables[comp.getTableDC()][HuffmanTable.TYPE_DC];

			int value = dcTable.decodeSymbol(mBitStream);

			if (value == -1)
			{
				return false;
			}

			if (value > 0)
			{
				entropy.last_dc_val[ci] += dcTable.readCoefficient(mBitStream, value);
			}

			aCoefficients[blockIndex][0] = entropy.last_dc_val[ci] << aJPEG.Al;
		}

		return true;
	}


	private boolean decode_mcu_AC_first(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		if (mEOBRun > 0)
		{
			mEOBRun--;
		}
		else
		{
			int ci = aJPEG.mMCUComponentIndices[0];
			int[] coefficients = aCoefficients[0];

			ComponentInfo comp = aJPEG.mComponentInfo[ci];

			HuffmanTable acTable = aJPEG.mHuffmanTables[comp.getTableAC()][HuffmanTable.TYPE_AC];

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
							mEOBRun = 1 << r;
							mEOBRun += mBitStream.readBits(r) - 1;
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
		for (int blockIndex = 0; blockIndex < aJPEG.mMCUBlockCount; blockIndex++)
		{
			if (mBitStream.readBits(1) != 0)
			{
				aCoefficients[blockIndex][0] |= 1 << aJPEG.Al;
			}
		}

		return true;
	}


	private boolean decode_mcu_AC_refine(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		int p1 = 1 << aJPEG.Al; // 1 in the bit position being coded
		int m1 = (-1) << aJPEG.Al; // -1 in the bit position being coded

		int ci = aJPEG.mMCUComponentIndices[0];
		ComponentInfo comp = aJPEG.mComponentInfo[ci];

		HuffmanTable acTable = aJPEG.mHuffmanTables[comp.getTableAC()][HuffmanTable.TYPE_AC];

		int k = aJPEG.Ss;
		int[] coefficients = aCoefficients[0];

		if (mEOBRun == 0)
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
						mEOBRun = 1 << r; // EOBr, run length is 2^r + appended bits
						if (r != 0)
						{
							r = mBitStream.readBits(r);
							mEOBRun += r;
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

		if (mEOBRun != 0)
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

			mEOBRun--; // Count one block completed in EOB run
		}

		return true;
	}


	private boolean decodeImpl(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		for (int blockIndex = 0; blockIndex < aJPEG.mMCUBlockCount; blockIndex++)
		{
			int ci = aJPEG.mMCUComponentIndices[blockIndex];
			ComponentInfo comp = aJPEG.mComponentInfo[ci];

			HuffmanTable dcTable = aJPEG.mHuffmanTables[comp.getTableDC()][HuffmanTable.TYPE_DC];
			HuffmanTable acTable = aJPEG.mHuffmanTables[comp.getTableAC()][HuffmanTable.TYPE_AC];

			Arrays.fill(aCoefficients[blockIndex], 0);

			int value = dcTable.decodeSymbol(mBitStream);

			if (value == -1)
			{
				return false;
			}

			if (value > 0)
			{
				aJPEG_entropy.last_dc_val[ci] += dcTable.readCoefficient(mBitStream, value) << aJPEG.Al;
			}

			aCoefficients[blockIndex][0] = aJPEG_entropy.last_dc_val[ci];

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