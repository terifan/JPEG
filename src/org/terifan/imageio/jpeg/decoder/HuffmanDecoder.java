package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import java.util.Arrays;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;
import static org.terifan.imageio.jpeg.decoder.JPEGImageReader.MAX_CHANNELS;


public class HuffmanDecoder extends Decoder
{
	private int[] mPreviousDCValue;
	private DHTSegment[][] mHuffmanTables;


	public HuffmanDecoder(BitInputStream aBitStream)
	{
		super(aBitStream);

		mPreviousDCValue = new int[MAX_CHANNELS];
		mHuffmanTables = new DHTSegment[MAX_CHANNELS][2];
	}


	@Override
	void initialize(JPEG aJPEG)
	{
	}


	@Override
	void startPass(JPEG aJPEG)
	{
		ArithEntropyState entropy = new ArithEntropyState();

		aJPEG.entropy = entropy;
		aJPEG.entropy.restarts_to_go = aJPEG.restart_interval;
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
//				for (int ci = 0; ci < aJPEG.num_components; ci++)
//				{
//					mPreviousDCValue[ci] = 0;
//				}
				for (int ci = 0; ci < aJPEG.comps_in_scan; ci++)
				{
					mPreviousDCValue[aJPEG.MCU_membership[ci]] = 0;
				}

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
				return aJPEG.Ss == 0 ? decode_mcu_DC_first(aJPEG, aCoefficients) : decode_mcu_AC_first(aJPEG, aCoefficients);
			}

			return aJPEG.Ss == 0 ? decode_mcu_DC_refine(aJPEG, aCoefficients) : decode_mcu_AC_refine(aJPEG, aCoefficients);
		}

		return decodeImpl(aJPEG, aCoefficients);
	}


	private boolean decode_mcu_DC_first(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		for (int blockIndex = 0; blockIndex < aJPEG.blocks_in_MCU; blockIndex++)
		{
			int ci = aJPEG.MCU_membership[blockIndex];
			ComponentInfo comp = aJPEG.cur_comp_info[ci];

			DHTSegment dcTable = mHuffmanTables[comp.getTableDC()][DHTSegment.TYPE_DC];

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
		}

		return true;
	}

	int EOBRUN;

	private boolean decode_mcu_AC_first(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
//		for (int blockIndex = 0; blockIndex < aJPEG.blocks_in_MCU; blockIndex++)
		{
		int blockIndex = 0;

			int ci = aJPEG.MCU_membership[blockIndex];

			Arrays.fill(aCoefficients[blockIndex], 0);

			if (EOBRUN > 0)
			{
				EOBRUN--;
				return true;
			}

			ComponentInfo comp = aJPEG.cur_comp_info[ci];

			DHTSegment acTable = mHuffmanTables[comp.getTableAC()][DHTSegment.TYPE_AC];

			for (int k = aJPEG.Ss; k <= aJPEG.Se; k++)
			{
				int value = acTable.decodeSymbol(mBitStream);

				if (value == -1)
				{
					return false;
				}

				int r = value >> 4;
				int s = value & 15;

				if (s > 0)
				{
					k += r;

					aCoefficients[blockIndex][NATURAL_ORDER[k]] = acTable.readCoefficient(mBitStream, s) << aJPEG.Al;
				}
				else
				{
					if (r != 15)
					{
						if (r == 0)
						{
							EOBRUN += mBitStream.readBits(r) - 1;
						}
					}
					break;
				}
				k += 15;
			}
		}

		return true;
	}


	private boolean decode_mcu_DC_refine(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		return decodeImpl(aJPEG, aCoefficients);
	}


	private boolean decode_mcu_AC_refine(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		return decodeImpl(aJPEG, aCoefficients);
	}


	private boolean decodeImpl(JPEG aJPEG, int[][] aCoefficients) throws IOException
	{
		for (int blockIndex = 0; blockIndex < aJPEG.blocks_in_MCU; blockIndex++)
		{
			int ci = aJPEG.MCU_membership[blockIndex];
			ComponentInfo comp = aJPEG.cur_comp_info[ci];

			DHTSegment dcTable = mHuffmanTables[comp.getTableDC()][DHTSegment.TYPE_DC];
			DHTSegment acTable = mHuffmanTables[comp.getTableAC()][DHTSegment.TYPE_AC];

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


	void readHuffmanTables() throws IOException
	{
		int length = mBitStream.readInt16() - 2;

		do
		{
			DHTSegment dht = new DHTSegment(mBitStream);

			mHuffmanTables[dht.getIdentity()][dht.getType()] = dht;

			length -= 17 + dht.getNumSymbols();

			if (length < 0)
			{
				throw new IOException("Error in JPEG stream; illegal DHT segment size.");
			}
		}
		while (length > 0);
	}
}