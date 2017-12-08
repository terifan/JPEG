package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;


public class SOSMarkerSegment
{
	private int mNumComponents;
	private int[] mComponents;
	private int[] mTableAC;
	private int[] mTableDC;
	private int mSs;
	private int mSe;
	private int mAh;
	private int mAl;


	public SOSMarkerSegment(BitInputStream aInputStream) throws IOException
	{
		int segmentLength = aInputStream.readInt16();

		mNumComponents = aInputStream.readInt8();

		if (6 + 2 * mNumComponents != segmentLength)
		{
			throw new IOException("Error in JPEG stream; illegal SOS segment size.");
		}

		mComponents = new int[mNumComponents];
		mTableAC = new int[mNumComponents];
		mTableDC = new int[mNumComponents];

		for (int i = 0; i < mNumComponents; i++)
		{
			mComponents[i] = aInputStream.readInt8();

			int temp = aInputStream.readInt8();
			mTableAC[i] = temp & 15;
			mTableDC[i] = temp >> 4;
		}

		mSs = aInputStream.readInt8();
		mSe = aInputStream.readInt8();
		mAh = aInputStream.readBits(4);
		mAl = aInputStream.readBits(4);

//		if (VERBOSE)
		{
			System.out.println("SOSMarkerSegment");
			System.out.println("  numcomponents=" + mNumComponents);
			for (int i = 0; i < mNumComponents; i++)
			{
				String component;
				switch (mComponents[i])
				{
					case ComponentInfo.Y:
						component = "Y";
						break;
					case ComponentInfo.CB:
						component = "Cb";
						break;
					case ComponentInfo.CR:
						component = "Cr";
						break;
					case ComponentInfo.I:
						component = "I";
						break;
					default:
						component = "Q";
				}

				System.out.println("  component=" + component + ", ac-table=" + mTableAC[i] + ", dc-table=" + mTableDC[i] + ", ss=" + mSs + ", se=" + mSe + ", ah=" + mAh + ", al=" + mAl);
			}
		}
	}


	public int getNumComponents()
	{
		return mNumComponents;
	}


	public int getComponent(int aIndex)
	{
		return mComponents[aIndex];
	}


	public int getACTable(int aIndex)
	{
		return mTableAC[aIndex];
	}


	public int getDCTable(int aIndex)
	{
		return mTableDC[aIndex];
	}


	public int getSs()
	{
		return mSs;
	}


	public int getSe()
	{
		return mSe;
	}


	public int getAh()
	{
		return mAh;
	}


	public int getAl()
	{
		return mAl;
	}
}