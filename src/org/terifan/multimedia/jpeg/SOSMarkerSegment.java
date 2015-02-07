package org.terifan.multimedia.jpeg;

import java.io.IOException;


class SOSMarkerSegment
{
	private int mNumComponents;
	private int[] mComponents;
	private int[] mHuffmanTableAC;
	private int[] mHuffmanTableDC;


	public SOSMarkerSegment(BitInputStream aInputStream) throws IOException
	{
		int segmentLength = aInputStream.readShort();

		mNumComponents = aInputStream.readByte();

		if (6 + 2 * mNumComponents != segmentLength)
		{
			throw new IOException("Error in JPEG stream; illegal SOS segment size.");
		}

		mComponents = new int[mNumComponents];
		mHuffmanTableAC = new int[mNumComponents];
		mHuffmanTableDC = new int[mNumComponents];

		for (int i = 0; i < mNumComponents; i++)
		{
			mComponents[i] = aInputStream.readByte();
			int temp = aInputStream.readByte();
			mHuffmanTableAC[i] = temp & 15;
			mHuffmanTableDC[i] = temp >> 4;
		}

		aInputStream.skip(3);

		if (JPEGImageReader.VERBOSE)
		{
			System.out.println("SOSMarkerSegment[numcomponents=" + mNumComponents + "]");
			for (int i = 0; i < mNumComponents; i++)
			{
				String component;
				switch (mComponents[i])
				{
					case SOFMarkerSegment.ComponentInfo.Y:
						component = "Y";
						break;
					case SOFMarkerSegment.ComponentInfo.Cb:
						component = "Cb";
						break;
					case SOFMarkerSegment.ComponentInfo.Cr:
						component = "Cr";
						break;
					case SOFMarkerSegment.ComponentInfo.I:
						component = "I";
						break;
					default:
						component = "Q";
				}

				System.out.println("  component=" + component + ", ac-table=" + mHuffmanTableAC[i] + ", dc-table=" + mHuffmanTableDC[i]);
			}
		}
	}


	public int getComponent(int aIndex)
	{
		return mComponents[aIndex];
	}


	public int getHuffmanTableAC(int aIndex)
	{
		return mHuffmanTableAC[aIndex];
	}


	public int getHuffmanTableDC(int aIndex)
	{
		return mHuffmanTableDC[aIndex];
	}
}