package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class SOSSegment extends Segment
{
	private int[] mComponentIds;
	private int[] mTableAC;
	private int[] mTableDC;


	public SOSSegment(int... aComponentIds)
	{
		mTableDC = new int[4];
		mTableAC = new int[4];
		mComponentIds = aComponentIds;
	}


	@Override
	public SOSSegment decode(JPEG aJPEG, BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16();

		aJPEG.mScanBlockCount = aBitStream.readInt8();

		if (6 + 2 * aJPEG.mScanBlockCount != length)
		{
			throw new IOException("Error in JPEG stream; illegal SOS segment size: " + length + ", offset " + aBitStream.getStreamOffset());
		}

		mComponentIds = new int[aJPEG.mScanBlockCount];

		for (int i = 0; i < aJPEG.mScanBlockCount; i++)
		{
			mComponentIds[i] = aBitStream.readInt8() /*+ 0*mJPEG.mAdjustComponentId*/;
			mTableDC[i] = aBitStream.readBits(4);
			mTableAC[i] = aBitStream.readBits(4);
		}

		aJPEG.Ss = aBitStream.readInt8();
		aJPEG.Se = aBitStream.readInt8();
		aJPEG.Ah = aBitStream.readBits(4);
		aJPEG.Al = aBitStream.readBits(4);
		aJPEG.mScanBlockCount = getNumComponents();

		return this;
	}


	@Override
	public SOSSegment encode(JPEG aJPEG, BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt16(SegmentMarker.SOS.CODE);
		aBitStream.writeInt16(2 + 1 + mComponentIds.length * 2 + 3);

		aBitStream.writeInt8(mComponentIds.length);

		for (int i = 0; i < mComponentIds.length; i++)
		{
			aBitStream.writeInt8(mComponentIds[i]);
			aBitStream.writeInt8((mTableDC[i] << 4)  + mTableAC[i]);
		}

		aBitStream.writeInt8(aJPEG.Ss);
		aBitStream.writeInt8(aJPEG.Se);
		aBitStream.writeInt8((aJPEG.Ah << 4) + aJPEG.Al);

		return this;
	}


	@Override
	public SOSSegment print(JPEG aJPEG, Log aLog) throws IOException
	{
		aLog.println("SOS segment");
		aLog.println("  coefficient partitioning");
		aLog.println("    ss=%d, se=%d, ah=%d, al=%d", aJPEG.Ss, aJPEG.Se, aJPEG.Ah, aJPEG.Al);

		for (int i = 0; i < aJPEG.mScanBlockCount; i++)
		{
			aLog.println("  component %s", ComponentInfo.Type.fromComponentId(mComponentIds[i]).name());
			aLog.println("    dc-table=%d, ac-table=%d", mTableDC[i], mTableAC[i]);
		}

		return this;
	}


	public int getComponentIdByIndex(int aIndex)
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


	public void prepareMCU(JPEG aJPEG)
	{
		aJPEG.mScanBlockCount = mComponentIds.length;

		aJPEG.mMCUBlockCount = 0;
		for (int scanComponentIndex = 0; scanComponentIndex < aJPEG.mScanBlockCount; scanComponentIndex++)
		{
			ComponentInfo comp = aJPEG.mSOFSegment.getComponentById(getComponentIdByIndex(scanComponentIndex));
			aJPEG.mMCUBlockCount += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		aJPEG.mMCUComponentIndices = new int[aJPEG.mMCUBlockCount];
		aJPEG.mComponentInfo = new ComponentInfo[aJPEG.mScanBlockCount];

		for (int scanComponentIndex = 0, blockIndex = 0; scanComponentIndex < aJPEG.mScanBlockCount; scanComponentIndex++)
		{
			ComponentInfo comp = aJPEG.mSOFSegment.getComponentById(getComponentIdByIndex(scanComponentIndex));

			comp.setTableAC(getACTable(scanComponentIndex));
			comp.setTableDC(getDCTable(scanComponentIndex));

			aJPEG.mComponentInfo[scanComponentIndex] = comp;

			for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, blockIndex++)
			{
				aJPEG.mMCUComponentIndices[blockIndex] = scanComponentIndex;
			}
		}
	}
}
