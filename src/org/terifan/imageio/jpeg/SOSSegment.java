package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;


public class SOSSegment
{
	private int mNumComponents;
	private int[] mComponentIds;
	private int[] mTableAC;
	private int[] mTableDC;
	private int mSs;
	private int mSe;
	private int mAh;
	private int mAl;


	public SOSSegment(BitInputStream aInputStream) throws IOException
	{
		int segmentLength = aInputStream.readInt16();

		mNumComponents = aInputStream.readInt8();

		if (6 + 2 * mNumComponents != segmentLength)
		{
			throw new IOException("Error in JPEG stream; illegal SOS segment size.");
		}

		mComponentIds = new int[mNumComponents];
		mTableDC = new int[mNumComponents];
		mTableAC = new int[mNumComponents];

		for (int i = 0; i < mNumComponents; i++)
		{
			mComponentIds[i] = aInputStream.readInt8();
			mTableDC[i] = aInputStream.readBits(4);
			mTableAC[i] = aInputStream.readBits(4);
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
				switch (mComponentIds[i])
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

				System.out.println("  SOS: component=" + component + ", dc-table=" + mTableDC[i] + ", ac-table=" + mTableAC[i] + ", ss=" + mSs + ", se=" + mSe + ", ah=" + mAh + ", al=" + mAl);
			}
		}
	}


	public int getNumComponents()
	{
		return mNumComponents;
	}


	public int getComponent(int aIndex)
	{
		return mComponentIds[aIndex];
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