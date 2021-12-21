package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class HuffmanTable
{
	public final static int TYPE_DC = 0;
	public final static int TYPE_AC = 1;

	private int mType;
	private int mIndex;
	private boolean mSent;

	// decoder

	private int mMaxLength;
	private int mNumSymbols;
	private int [] mLookup;

	// encoder

	// These two fields directly represent the contents of a JPEG DHT marker bits[k] = # of symbols with codes of  length k bits; bits[0] is unused
	public int[] bits;
	// The symbols, in order of incr code length */
	public int[] huffval;


	public HuffmanTable()
	{
	}


	public HuffmanTable(int aType, int aIndex)
	{
		mType = aType;
		mIndex = aIndex;
		bits = new int[17];
		huffval = new int[256];
	}


	public HuffmanTable(int aType, int aIndex, int[] bits, int[] huffval)
	{
		mType = aType;
		mIndex = aIndex;
		this.bits = bits;
		this.huffval = huffval;

		mMaxLength = 0;
		mNumSymbols = 0;

		for (int i = 1; i <= 16; i++)
		{
			mNumSymbols += bits[i];
			if (bits[i] > 0)
			{
				mMaxLength = i + 1;
			}
		}
	}


	HuffmanTable decode(InputStream aInput) throws IOException
	{
		int temp = aInput.read();
		mIndex = temp & 0x07;
		mType = (temp & 0x10) == 0 ? TYPE_DC : TYPE_AC;

		int[] counts = new int[17];

		for (int i = 1; i <= 16; i++)
		{
			counts[i] = aInput.read();
			mNumSymbols += counts[i];
			if (counts[i] > 0)
			{
				mMaxLength = i + 1;
			}
		}

		mLookup = new int[1 << mMaxLength];

		for (int length = 1, code = 0; length < 17; length++, code <<= 1)
		{
			for (int j = 0; j < counts[length]; j++, code++)
			{
				int symbol = aInput.read();
				int shift = 1 << (mMaxLength - length);

				for (int k = 0; k < shift; k++)
				{
					mLookup[(code * shift) | k] = (length << 16) + symbol;
				}
			}
		}

		return this;
	}


	void encode(OutputStream aOutput) throws IOException
	{
		if (mSent)
		{
			return;
		}

		aOutput.write(mIndex + (mType == TYPE_AC ? 0x10 : 0x00));

		{
			int length = 0;
			for (int i = 1; i <= 16; i++)
			{
				length += bits[i];
			}

			for (int i = 1; i <= 16; i++)
			{
				aOutput.write(bits[i]);
			}

			for (int i = 0; i < length; i++)
			{
				aOutput.write(huffval[i]);
			}
		}

		mSent = true;
	}


	public int getIndex()
	{
		return mIndex;
	}


	public int getType()
	{
		return mType;
	}


	public int getNumSymbols()
	{
		return mNumSymbols;
	}


	public int getMaxLength()
	{
		return mMaxLength;
	}


	public int decodeSymbol(BitInputStream aBitStream) throws IOException
	{
		int p = aBitStream.peekBits(mMaxLength);
		int s = mLookup[p];
		int l = s >> 16;
		int code = s & 0xffff;

		if (l == 0)
		{
			throw new IllegalStateException(p+" "+s+" "+l+" -- ("+code+" >> 16) == 0");
		}

		aBitStream.skipBits(l);
		return code;
	}


	public int readCoefficient(BitInputStream aBitStream, int aLength) throws IOException
	{
		int symbol = aBitStream.readBits(aLength);

		if (symbol < 1 << (aLength - 1))
		{
			return symbol + (-1 << aLength) + 1;
		}

		return symbol;
	}


	public void print(Log aLog)
	{
		aLog.println("  HuffmanTable");
		aLog.println("    identity=%d, type=%s, numSymbols=%d, maxLength=%d", mIndex, mType == TYPE_AC ? "AC" : "DC", mNumSymbols, mNumSymbols);

		for (int length = 1, code = 0, n = 0; length < 17; length++, code <<= 1)
		{
			for (int j = 0; j < bits[length]; j++, code++)
			{
				int symbol = huffval[n++];
				int shift = 1 << (mMaxLength - length);

				String s = "";
				for (int z = mMaxLength, k = 0; --z >= 0 && k < length; k++)
				{
					s += 1 & ((code * shift) >> (mMaxLength-k-1));
				}
				aLog.println("%-"+mMaxLength+"s [%d] = %d", s, length, symbol);
			}
		}
	}


	public boolean isSent()
	{
		return mSent;
	}
}
