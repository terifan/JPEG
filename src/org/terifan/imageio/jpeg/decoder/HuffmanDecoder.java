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
	void jinit_decoder(JPEG aCinfo)
	{
	}


	@Override
	void start_pass(JPEG aCinfo)
	{
	}


	@Override
	void finish_pass(JPEG aCinfo)
	{
	}


	@Override
	boolean decode_mcu(JPEG aCinfo, int[][] aCoefficients) throws IOException
	{
		for (int blockIndex = 0; blockIndex < aCinfo.blocks_in_MCU; blockIndex++)
		{
			int component = aCinfo.MCU_membership[blockIndex];

			ComponentInfo comp = aCinfo.cur_comp_info[component];

			DHTSegment dcTable = mHuffmanTables[comp.getTableDC()][DHTSegment.TYPE_DC];
			DHTSegment acTable = mHuffmanTables[comp.getTableAC()][DHTSegment.TYPE_AC];

			if (!decodeImpl(aCinfo, aCoefficients[blockIndex], component, dcTable, acTable))
			{
				return false;
			}
		}

		return true;
	}


//		for (int component = 0; component < numComponents; component++)
//		{
//			ComponentInfo comp = mSOFMarkerSegment.getComponent(component);
//			int samplingX = comp.getTableDC();
//			int samplingY = comp.getTableAC();
//
//			for (int cy = 0; cy < samplingY; cy++)
//			{
//				for (int cx = 0; cx < samplingX; cx++)
//				{
//					if (!readDCTCofficients(aCoefficients[cy][cx][component], component))
//					{
//						return false;
//					}
//				}
//			}
//		}


	boolean decodeImpl(JPEG aCinfo, int[] aCoefficients, int aComponent, DHTSegment dcTable, DHTSegment acTable) throws IOException
	{
		Arrays.fill(aCoefficients, 0);

		int value = dcTable.decodeSymbol(mBitStream);

		if (value == -1)
		{
			return false;
		}

		if (value > 0)
		{
			mPreviousDCValue[aComponent] += dcTable.readCoefficient(mBitStream, value);
		}

		aCoefficients[NATURAL_ORDER[0]] = mPreviousDCValue[aComponent];

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
				aCoefficients[NATURAL_ORDER[offset]] = acTable.readCoefficient(mBitStream, codeLength);
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