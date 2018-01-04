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


	public SOSSegment(JPEG aJPEG, int... aComponentIds)
	{
		mJPEG = aJPEG;
		mTableDC = new int[4];
		mTableAC = new int[4];
		mComponentIds = aComponentIds;
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
		System.out.println("  numComponents=" + mJPEG.comps_in_scan);

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
		aBitStream.writeInt16(2 + 1 + mComponentIds.length * 2 + 3);

		aBitStream.writeInt8(mComponentIds.length);

		for (int i = 0; i < mComponentIds.length; i++)
		{
			aBitStream.writeInt8(mComponentIds[i]);
			aBitStream.writeBits(mTableDC[i], 4);
			aBitStream.writeBits(mTableAC[i], 4);
		}

		aBitStream.writeInt8(mJPEG.Ss);
		aBitStream.writeInt8(mJPEG.Se);
		aBitStream.writeBits(mJPEG.Ah, 4);
		aBitStream.writeBits(mJPEG.Al, 4);

		return this;
	}


	public int getComponentByIndex(int aIndex)
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


	public void prepareMCU()
	{
		mJPEG.comps_in_scan = mComponentIds.length;
		mJPEG.blocks_in_MCU = 0;

		for (int scanComponentIndex = 0; scanComponentIndex < mJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = mJPEG.mSOFSegment.getComponentById(getComponentByIndex(scanComponentIndex));
			mJPEG.blocks_in_MCU += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		mJPEG.MCU_membership = new int[mJPEG.blocks_in_MCU];
		mJPEG.cur_comp_info = new ComponentInfo[mJPEG.comps_in_scan];

		if (VERBOSE)
		{
			System.out.println("MCU");
		}

		for (int scanComponentIndex = 0, blockIndex = 0; scanComponentIndex < mJPEG.comps_in_scan; scanComponentIndex++)
		{
			ComponentInfo comp = mJPEG.mSOFSegment.getComponentById(getComponentByIndex(scanComponentIndex));
			comp.setTableAC(getACTable(scanComponentIndex));
			comp.setTableDC(getDCTable(scanComponentIndex));

			mJPEG.cur_comp_info[scanComponentIndex] = comp;

			for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, blockIndex++)
			{
				mJPEG.MCU_membership[blockIndex] = scanComponentIndex;
			}

			if (VERBOSE)
			{
				System.out.println("  " + comp);
			}
		}
	}
}