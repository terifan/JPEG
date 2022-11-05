package org.terifan.imageio.jpeg;

import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DACSegment extends Segment
{
	private SOSSegment mSOSSegment;


	public DACSegment()
	{
	}


	public DACSegment(SOSSegment aSOSSegment)
	{
		mSOSSegment = aSOSSegment;
	}


	@Override
	public DACSegment decode(JPEG aJPEG, BitInputStream aBitStream) throws IOException
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
				aJPEG.mArithACK[index - NUM_ARITH_TBLS] = val;
			}
			else // define DC table
			{
				aJPEG.mArithDCU[index] = val >> 4;
				aJPEG.mArithDCL[index] = val & 0x0F;

				if (aJPEG.mArithDCL[index] > aJPEG.mArithDCU[index])
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
	public DACSegment encode(JPEG aJPEG, BitOutputStream aBitStream) throws IOException
	{
		int ac = 0;
		for (int i = 0; i < aJPEG.mArithACK.length; i++)
		{
			boolean found = false;
			for (int scanComponentIndex = 0; !found && scanComponentIndex < aJPEG.mScanBlockCount; scanComponentIndex++)
			{
				found = mSOSSegment.getACTable(scanComponentIndex) == i;
			}

			if (found)
			{
				ac++;
			}
		}

		aBitStream.writeInt16(SegmentMarker.DAC.CODE);
		aBitStream.writeInt16(2 + 2 * (aJPEG.mArithDCU.length + ac));

		for (int i = 0; i < aJPEG.mArithDCU.length; i++)
		{
			aBitStream.writeInt8(i);
			aBitStream.writeInt8((aJPEG.mArithDCU[i] << 4) + aJPEG.mArithDCL[i]);
		}
		for (int i = 0; i < aJPEG.mArithACK.length; i++)
		{
			boolean found = false;
			for (int scanComponentIndex = 0; !found && scanComponentIndex < aJPEG.mScanBlockCount; scanComponentIndex++)
			{
				found = mSOSSegment.getACTable(scanComponentIndex) == i;
			}

			if (found)
			{
				aBitStream.writeInt8(NUM_ARITH_TBLS + i);
				aBitStream.writeInt8(aJPEG.mArithACK[i]);
			}
		}

		return this;
	}


	@Override
	public DACSegment print(JPEG aJPEG, Log aLog) throws IOException
	{
		aLog.println("DAC segment");

		aLog.println("  DC=%d, AC=%d", aJPEG.mArithDCU.length, aJPEG.mArithACK.length);

		if (aLog.isDetailed())
		{
			for (int i = 0; i < aJPEG.mArithDCU.length; i++)
			{
				aLog.println("    DC %d", i);
				aLog.println("      u=%d, l=%d", aJPEG.mArithDCU[i], aJPEG.mArithDCL[i]);
			}

			for (int i = 0; i < aJPEG.mArithACK.length; i++)
			{
				boolean found = false;
				for (int scanComponentIndex = 0; !found && scanComponentIndex < aJPEG.mScanBlockCount; scanComponentIndex++)
				{
					found = mSOSSegment.getACTable(scanComponentIndex) == i;
				}

				if (found)
				{
					aLog.println("    AC %d", i);
					aLog.println("      k=%d", aJPEG.mArithACK[i]);
				}
			}
		}

		return this;
	}
}
