package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class SOSSegment extends Segment
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


	@Override
	public SOSSegment decode(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16();

		mJPEG.mScanBlockCount = aBitStream.readInt8();

		if (6 + 2 * mJPEG.mScanBlockCount != length)
		{
			throw new IOException("Error in JPEG stream; illegal SOS segment size: " + length + ", offset " + aBitStream.getStreamOffset());
		}

		mComponentIds = new int[mJPEG.mScanBlockCount];

		for (int i = 0; i < mJPEG.mScanBlockCount; i++)
		{
			mComponentIds[i] = aBitStream.readInt8();
			mTableDC[i] = aBitStream.readBits(4);
			mTableAC[i] = aBitStream.readBits(4);
		}

		mJPEG.Ss = aBitStream.readInt8();
		mJPEG.Se = aBitStream.readInt8();
		mJPEG.Ah = aBitStream.readBits(4);
		mJPEG.Al = aBitStream.readBits(4);
		mJPEG.mScanBlockCount = getNumComponents();

		return this;
	}


	@Override
	public SOSSegment encode(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt16(SegmentMarker.SOS.CODE);
		aBitStream.writeInt16(2 + 1 + mComponentIds.length * 2 + 3);

		aBitStream.writeInt8(mComponentIds.length);

		for (int i = 0; i < mComponentIds.length; i++)
		{
			aBitStream.writeInt8(mComponentIds[i]);
			aBitStream.writeInt8((mTableDC[i] << 4)  + mTableAC[i]);
		}

		aBitStream.writeInt8(mJPEG.Ss);
		aBitStream.writeInt8(mJPEG.Se);
		aBitStream.writeInt8((mJPEG.Ah << 4) + mJPEG.Al);

		return this;
	}


	@Override
	public SOSSegment print(Log aLog) throws IOException
	{
		aLog.println("SOS segment");
		aLog.println("  coefficient partitioning");
		aLog.println("    ss=%d, se=%d, ah=%d, al=%d", mJPEG.Ss, mJPEG.Se, mJPEG.Ah, mJPEG.Al);

		for (int i = 0; i < mJPEG.mScanBlockCount; i++)
		{
			aLog.println("  component %s", ComponentInfo.Type.values()[mComponentIds[i] - 0*1].name());
			aLog.println("    dc-table=%d, ac-table=%d", mTableDC[i], mTableAC[i]);
		}

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
		mJPEG.mScanBlockCount = mComponentIds.length;
		mJPEG.mMCUBlockCount = 0;

		for (int scanComponentIndex = 0; scanComponentIndex < mJPEG.mScanBlockCount; scanComponentIndex++)
		{
			ComponentInfo comp = mJPEG.mSOFSegment.getComponentById(getComponentByIndex(scanComponentIndex));
			mJPEG.mMCUBlockCount += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		mJPEG.mMCUComponentIndices = new int[mJPEG.mMCUBlockCount];
		mJPEG.mComponentInfo = new ComponentInfo[mJPEG.mScanBlockCount];

		for (int scanComponentIndex = 0, blockIndex = 0; scanComponentIndex < mJPEG.mScanBlockCount; scanComponentIndex++)
		{
			ComponentInfo comp = mJPEG.mSOFSegment.getComponentById(getComponentByIndex(scanComponentIndex));
			comp.setTableAC(getACTable(scanComponentIndex));
			comp.setTableDC(getDCTable(scanComponentIndex));

			mJPEG.mComponentInfo[scanComponentIndex] = comp;

			for (int i = 0; i < comp.getHorSampleFactor() * comp.getVerSampleFactor(); i++, blockIndex++)
			{
				mJPEG.mMCUComponentIndices[blockIndex] = scanComponentIndex;
			}
		}
	}
}