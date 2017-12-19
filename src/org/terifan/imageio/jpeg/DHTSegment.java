package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.util.Arrays;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;


public class DHTSegment
{
	public final static int TYPE_DC = 0;
	public final static int TYPE_AC = 1;

	private int mNumSymbols;
	private int mType;
	private int mIdentity;
	private int [] mLookup;
	private int mMaxLength;


	public DHTSegment(BitInputStream aInputStream) throws IOException
	{
		int temp = aInputStream.readInt8();
		mIdentity = temp & 0x07;
		mType = (temp & 16) == 0 ? TYPE_DC : TYPE_AC;

		int[] counts = new int[16];

		for (int i = 0; i < 16; i++)
		{
			counts[i] = aInputStream.readInt8();
			mNumSymbols += counts[i];
			if (counts[i] > 0)
			{
				mMaxLength = i + 1;
			}
		}

		mLookup = new int[1 << mMaxLength];

		System.out.println("------------------");

		for (int i = 0, code = 0; i < 16; i++)
		{
			for (int j = 0; j < counts[i]; j++)
			{
				int length = i + 1;
				int symbol = aInputStream.readInt8();
				int sz = 1 << (mMaxLength - length);

				String s = "";
				for (int z = length; --z >= 0; )
				{
					s += 1 & ((code * sz) >> z);
				}
				String s2 = "";
				for (int z = length; --z >= 0; )
				{
					s2 += 1 & ((code * sz + sz-1) >> z);
				}
				System.out.printf("%"+mMaxLength+"s -- %"+mMaxLength+"s [%d] = %d%n", s, s2, length, symbol);

				for (int k = 0; k < sz; k++)
				{
					mLookup[(code * sz) | k] = (length << 16) + symbol;
				}

				code++;
			}
			code <<= 1;
		}

		System.out.println("------------------");

//		if (VERBOSE)
		{
			System.out.println("DHTMarkerSegment[identity=" + mIdentity + ", type=" + (mType == TYPE_AC ? "AC" : "DC") + ", numsymbols=" + mNumSymbols + ", maxLength=" + mMaxLength + "]");
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


	public int decodeSymbol(BitInputStream aBitStream) throws IOException
	{
		int p = aBitStream.peekBits(mMaxLength);
		int s = mLookup[p];
		int l = s >> 16;
		int code = s & 0xffff;

		if (l == 0)
		{
//			code = aBitStream.readBits(1);
//
//			while (code > htbl->maxcode[l])
//			{
//			  code <<= 1;
//			  code |= aBitStream.readBits(1);
//			  l++;
//			}
//
//			return code;

			throw new IllegalStateException(p+" -- ("+code+" >> 16) == 0");
		}

		aBitStream.skipBits(l);
//		System.out.print("%"+(s >> 16)+"="+(s&0xff)+"% ");
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
}