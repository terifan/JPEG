package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import java.util.Arrays;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DHTMarkerSegment;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;
import org.terifan.imageio.jpeg.SOFMarkerSegment;
import static org.terifan.imageio.jpeg.decoder.JPEGImageReader.MAX_CHANNELS;


public class HuffmanDecoder extends Decoder
{
	private JPEGImageReader mImageReader;
	private int[] mPreviousDCValue;
	private DHTMarkerSegment[][] mHuffmanTables;


	public HuffmanDecoder(BitInputStream aBitStream, JPEGImageReader aImageReader)
	{
		super(aBitStream);

		mPreviousDCValue = new int[MAX_CHANNELS];
		mImageReader = aImageReader;
		mHuffmanTables = new DHTMarkerSegment[MAX_CHANNELS][2];
	}


	@Override
	void jinit_decoder(DecompressionState aCinfo)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}


	@Override
	void start_pass(DecompressionState aCinfo)
	{
	}


	@Override
	void finish_pass(DecompressionState aCinfo)
	{
	}


	@Override
	boolean decode_mcu(DecompressionState aCinfo, int[][] aCoefficients) throws IOException
	{
		for (int i = 0; i < aCinfo.num_components; i++)
		{
			if (!decodeImpl(aCinfo, aCoefficients, i))
			{
				return false;
			}
		}

		return true;
	}


	boolean decodeImpl(DecompressionState aCinfo, int[][] aCoefficients, int aComponent) throws IOException
	{
		int[] coefficients = aCoefficients[aComponent];

		Arrays.fill(coefficients, 0);

		DHTMarkerSegment dcTable = mHuffmanTables[aCinfo.cur_comp_info[aComponent].getTableDC()][0];
		DHTMarkerSegment acTable = mHuffmanTables[aCinfo.cur_comp_info[aComponent].getTableAC()][1];

		int value = dcTable.decodeSymbol(mBitStream);

		if (value == -1)
		{
			return false;
		}

		if (value > 0)
		{
			mPreviousDCValue[aComponent] += dcTable.readCoefficient(mBitStream, value);
		}

		coefficients[NATURAL_ORDER[0]] = mPreviousDCValue[aComponent];

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
				coefficients[NATURAL_ORDER[offset]] = acTable.readCoefficient(mBitStream, codeLength);
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
			DHTMarkerSegment dht = new DHTMarkerSegment(mBitStream);

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