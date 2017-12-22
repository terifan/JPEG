package org.terifan.imageio.jpeg;

import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class SOSSegment
{
	private int[] mComponentIds;
	private int[] mTableAC;
	private int[] mTableDC;
	private JPEG mJPEG;


	public SOSSegment(JPEG aJPEG, int... aComponents)
	{
		mJPEG = aJPEG;
		mTableDC = new int[4];
		mTableAC = new int[4];
		mComponentIds = aComponents;
	}


	public SOSSegment read(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16();

		mJPEG.comps_in_scan = aBitStream.readInt8();

		if (6 + 2 * mJPEG.comps_in_scan != length)
		{
			throw new IOException("Error in JPEG stream; illegal SOS segment size: " + length + ", offset " + aBitStream.getStreamOffset());
		}

		mComponentIds = new int[mJPEG.comps_in_scan];

		for (int i = 0; i < mJPEG.comps_in_scan; i++)
		{
			mComponentIds[i] = aBitStream.readInt8();
			mTableDC[i] = aBitStream.readBits(4);
			mTableAC[i] = aBitStream.readBits(4);
		}

		mJPEG.Ss = aBitStream.readInt8();
		mJPEG.Se = aBitStream.readInt8();
		mJPEG.Ah = aBitStream.readBits(4);
		mJPEG.Al = aBitStream.readBits(4);
		mJPEG.comps_in_scan = getNumComponents();

		if (VERBOSE)
		{
			log();
		}

		return this;
	}


	public void log()
	{
		System.out.println("SOSMarkerSegment");
		System.out.println("  numcomponents=" + mJPEG.comps_in_scan);

		for (int i = 0; i < mJPEG.comps_in_scan; i++)
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
				case ComponentInfo.Q:
					component = "Q";
					break;
				default:
					component = "<error>";
			}

			System.out.println("    component=" + component + ", dc-table=" + mTableDC[i] + ", ac-table=" + mTableAC[i] + ", ss=" + mJPEG.Ss + ", se=" + mJPEG.Se + ", ah=" + mJPEG.Ah + ", al=" + mJPEG.Al);
		}
	}


	public SOSSegment write(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt16(JPEGConstants.SOS);
		aBitStream.writeInt16(2 + 1 + mJPEG.comps_in_scan * 2 + 3);

		aBitStream.writeInt8(mJPEG.comps_in_scan);

		for (int i = 0; i < mJPEG.comps_in_scan; i++)
		{
			aBitStream.writeInt8(mJPEG.cur_comp_info[i].getComponentId());
			aBitStream.writeBits(mJPEG.cur_comp_info[i].getTableDC(), 4);
			aBitStream.writeBits(mJPEG.cur_comp_info[i].getTableAC(), 4);
		}

		aBitStream.writeInt8(mJPEG.Ss);
		aBitStream.writeInt8(mJPEG.Se);
		aBitStream.writeBits(mJPEG.Ah, 4);
		aBitStream.writeBits(mJPEG.Al, 4);

		return this;
	}


	public int getComponent(int aIndex)
	{
		return mComponentIds[aIndex];
	}


	public int getNumComponents()
	{
		return mComponentIds.length;
	}


	public int getACTable(int aIndex)
	{
		return mTableAC[aIndex];
	}


	public int getDCTable(int aIndex)
	{
		return mTableDC[aIndex];
	}


	public SOSSegment setTableAC(int aIndex, int aTableAC)
	{
		mTableAC[aIndex] = aTableAC;
		return this;
	}


	public SOSSegment setTableDC(int aIndex, int aTableDC)
	{
		mTableDC[aIndex] = aTableDC;
		return this;
	}
}