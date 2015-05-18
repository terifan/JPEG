package org.terifan.multimedia.jpeg;

import java.io.IOException;


class DHTMarkerSegment
{
	public final static int TYPE_DC = 0;
	public final static int TYPE_AC = 1;

	private int mNumSymbols;
	private int mType;
	private int mIdentity;

	private int [] mLookup;
	private int mMaxLength;


	public DHTMarkerSegment(BitInputStream aInputStream) throws IOException
	{
		int temp = aInputStream.readByte();
		mIdentity = temp & 0x07;
		mType = (temp & 16) == 0 ? TYPE_DC : TYPE_AC;

		int[] counts = new int[16];

		for (int i = 0; i < 16; i++)
		{
			counts[i] = aInputStream.readByte();
			mNumSymbols += counts[i];
			if (counts[i] > 0)
			{
				mMaxLength = i + 1;
			}
		}

		mLookup = new int[1 << mMaxLength];

		for (int i = 0, code = 0; i < 16; i++)
		{
			for (int j = 0; j < counts[i]; j++)
			{
				int length = i + 1;
				int value = aInputStream.readByte();

				for (int k = 0, sz = 1 << (mMaxLength - length); k < sz; k++)
				{
					mLookup[(code << (mMaxLength - length)) | k] = value + (length << 16);
				}

				code++;
			}
			code <<= 1;
		}

		if (JPEGImageReader.VERBOSE)
		{
			System.out.println("DHTMarkerSegment[identity=" + mIdentity + ", type=" + (mType == TYPE_AC ? "AC" : "DC") + ", numsymbols=" + mNumSymbols + "]");
		}
	}


	public int getIdentity()
	{
		return mIdentity;
	}


	public int getType()
	{
		return mType;
	}


	public int getNumSymbols()
	{
		return mNumSymbols;
	}


	public int decodeSymbol(BitInputStream aInputStream) throws IOException
	{
		int s = mLookup[aInputStream.peekBits(mMaxLength)];
		aInputStream.skipBits(s >> 16);
		return s & 65535;
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
}