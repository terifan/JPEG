package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


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
	private JPEG mJPEG;


	public SOSSegment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public SOSSegment read(BitInputStream aBitStream) throws IOException
	{
		int segmentLength = aBitStream.readInt16();

		mNumComponents = aBitStream.readInt8();

		if (6 + 2 * mNumComponents != segmentLength)
		{
			throw new IOException("Error in JPEG stream; illegal SOS segment size.");
		}

		mComponentIds = new int[mNumComponents];
		mTableDC = new int[mNumComponents];
		mTableAC = new int[mNumComponents];

		for (int i = 0; i < mNumComponents; i++)
		{
			mComponentIds[i] = aBitStream.readInt8();
			mTableDC[i] = aBitStream.readBits(4);
			mTableAC[i] = aBitStream.readBits(4);
		}

		mSs = aBitStream.readInt8();
		mSe = aBitStream.readInt8();
		mAh = aBitStream.readBits(4);
		mAl = aBitStream.readBits(4);

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

		return this;
	}


	public SOSSegment write(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt16(JPEGConstants.SOS);
		aBitStream.writeInt16(2 + 1 + mNumComponents * 2 + 3);

		aBitStream.writeInt8(mNumComponents);

		for (int i = 0; i < mNumComponents; i++)
		{
			aBitStream.writeInt8(mComponentIds[i]);
			aBitStream.writeBits(mTableDC[i], 4);
			aBitStream.writeBits(mTableAC[i], 4);
		}

		aBitStream.writeInt8(mSs);
		aBitStream.writeInt8(mSe);
		aBitStream.writeBits(mAh, 4);
		aBitStream.writeBits(mAl, 4);

		return this;
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