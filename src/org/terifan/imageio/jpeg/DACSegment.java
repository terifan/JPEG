package org.terifan.imageio.jpeg;

import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DACSegment extends Segment
{
	private JPEG mJPEG;
	private SOSSegment mSOSSegment;


	public DACSegment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public DACSegment(JPEG aJPEG, SOSSegment aSOSSegment)
	{
		mJPEG = aJPEG;
		mSOSSegment = aSOSSegment;
	}


	@Override
	public DACSegment decode(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		while (length > 0)
		{
			int index = aBitStream.readInt8();
			int val = aBitStream.readInt8();
			length -= 2;

			if (index < 0 || index >= (2 * NUM_ARITH_TBLS))
			{
				throw new IllegalArgumentException("Bad DAC index: " + index);
			}

			if (index >= NUM_ARITH_TBLS) // define AC table
			{
				mJPEG.mArithACK[index - NUM_ARITH_TBLS] = val;
			}
			else // define DC table
			{
				mJPEG.mArithDCU[index] = val >> 4;
				mJPEG.mArithDCL[index] = val & 0x0F;

				if (mJPEG.mArithDCL[index] > mJPEG.mArithDCU[index])
				{
					throw new IllegalArgumentException("Bad DAC value: " + val);
				}
			}
		}

		if (length != 0)
		{
			throw new IllegalArgumentException("Bad DAC segment: remaining: " + length);
		}

		return this;
	}


	@Override
	public DACSegment encode(BitOutputStream aBitStream) throws IOException
	{
		int ac = 0;
		for (int i = 0; i < mJPEG.mArithACK.length; i++)
		{
			boolean found = false;
			for (int scanComponentIndex = 0; !found && scanComponentIndex < mJPEG.mScanBlockCount; scanComponentIndex++)
			{
				found = mSOSSegment.getACTable(scanComponentIndex) == i;
			}

			if (found)
			{
				ac++;
			}
		}

		aBitStream.writeInt16(SegmentMarker.DAC.CODE);
		aBitStream.writeInt16(2 + 2 * (mJPEG.mArithDCU.length + ac));

		for (int i = 0; i < mJPEG.mArithDCU.length; i++)
		{
			aBitStream.writeInt8(i);
			aBitStream.writeInt8((mJPEG.mArithDCU[i] << 4) + mJPEG.mArithDCL[i]);
		}
		for (int i = 0; i < mJPEG.mArithACK.length; i++)
		{
			boolean found = false;
			for (int scanComponentIndex = 0; !found && scanComponentIndex < mJPEG.mScanBlockCount; scanComponentIndex++)
			{
				found = mSOSSegment.getACTable(scanComponentIndex) == i;
			}

			if (found)
			{
				aBitStream.writeInt8(NUM_ARITH_TBLS + i);
				aBitStream.writeInt8(mJPEG.mArithACK[i]);
			}
		}

		return this;
	}


	@Override
	public DACSegment print(Log aLog) throws IOException
	{
		aLog.println("DAC segment");

		aLog.println("  DC=%d, AC=%d", mJPEG.mArithDCU.length, mJPEG.mArithACK.length);

		if (aLog.isDetailed())
		{
			for (int i = 0; i < mJPEG.mArithDCU.length; i++)
			{
				aLog.println("    DC %d", i);
				aLog.println("      u=%d, l=%d", mJPEG.mArithDCU[i], mJPEG.mArithDCL[i]);
			}

			for (int i = 0; i < mJPEG.mArithACK.length; i++)
			{
				boolean found = false;
				for (int scanComponentIndex = 0; !found && scanComponentIndex < mJPEG.mScanBlockCount; scanComponentIndex++)
				{
					found = mSOSSegment.getACTable(scanComponentIndex) == i;
				}

				if (found)
				{
					aLog.println("    AC %d", i);
					aLog.println("      k=%d", mJPEG.mArithACK[i]);
				}
			}
		}

		return this;
	}
}
