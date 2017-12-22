package org.terifan.imageio.jpeg;

import java.io.IOException;
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


	public DHTSegment(BitInputStream aBitStream) throws IOException
	{
		int temp = aBitStream.readInt8();
		mIdentity = temp & 0x07;
		mType = (temp & 16) == 0 ? TYPE_DC : TYPE_AC;

		int[] counts = new int[17];

		for (int i = 1; i <= 16; i++)
		{
			counts[i] = aBitStream.readInt8();
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
				int symbol = aBitStream.readInt8();
				int shift = 1 << (mMaxLength - length);

//				String s = "";
//				for (int z = mMaxLength, k = 0; --z >= 0 && k < length; k++)
//				{
//					s += 1 & ((code * shift) >> (mMaxLength-k-1));
//				}
//				System.out.printf("%-"+mMaxLength+"s [%d] = %d%n", s, length, symbol);

				for (int k = 0; k < shift; k++)
				{
					mLookup[(code * shift) | k] = (length << 16) + symbol;
				}
			}
		}

		if (VERBOSE)
		{
			System.out.println("DHTMarkerSegment");
			System.out.println("  identity=" + mIdentity);
			System.out.println("  type=" + (mType == TYPE_AC ? "AC" : "DC"));
			System.out.println("  numsymbols=" + mNumSymbols);
			System.out.println("  maxLength=" + mMaxLength);
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
//			aBitStream.drop();
//
//			p = aBitStream.peekBits(mMaxLength);
//			s = mLookup[p];
//			l = s >> 16;
//			code = s & 0xffff;

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
}