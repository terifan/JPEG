package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.PrintStream;
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
				mJPEG.arith_ac_K[index - NUM_ARITH_TBLS] = val;
			}
			else // define DC table
			{
				mJPEG.arith_dc_U[index] = val >> 4;
				mJPEG.arith_dc_L[index] = val & 0x0F;

				if (mJPEG.arith_dc_L[index] > mJPEG.arith_dc_U[index])
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
		for (int i = 0; i < mJPEG.arith_ac_K.length; i++)
		{
			boolean found = false;
			for (int scanComponentIndex = 0; !found && scanComponentIndex < mJPEG.comps_in_scan; scanComponentIndex++)
			{
				found = mSOSSegment.getACTable(scanComponentIndex) == i;
			}

			if (found)
			{
				ac++;
			}
		}

		aBitStream.writeInt16(SegmentMarker.DAC.CODE);
		aBitStream.writeInt16(2 + 2 * (mJPEG.arith_dc_U.length + ac));

		for (int i = 0; i < mJPEG.arith_dc_U.length; i++)
		{
			aBitStream.writeInt8(i);
			aBitStream.writeInt8((mJPEG.arith_dc_U[i] << 4) + mJPEG.arith_dc_L[i]);
		}
		for (int i = 0; i < mJPEG.arith_ac_K.length; i++)
		{
			boolean found = false;
			for (int scanComponentIndex = 0; !found && scanComponentIndex < mJPEG.comps_in_scan; scanComponentIndex++)
			{
				found = mSOSSegment.getACTable(scanComponentIndex) == i;
			}

			if (found)
			{
				aBitStream.writeInt8(NUM_ARITH_TBLS + i);
				aBitStream.writeInt8(mJPEG.arith_ac_K[i]);
			}
		}

		return this;
	}


	@Override
	public DACSegment print(Log aLog) throws IOException
	{
		aLog.println("DAC segment");

		aLog.println("  DC=" + mJPEG.arith_dc_U.length + ", AC=" + mJPEG.arith_ac_K.length);

		if (aLog.isDetailed())
		{
			for (int i = 0; i < mJPEG.arith_dc_U.length; i++)
			{
				aLog.println("    DC " + i);
				aLog.println("      u=" + mJPEG.arith_dc_U[i] + ", l=" + mJPEG.arith_dc_L[i]);
			}

			for (int i = 0; i < mJPEG.arith_ac_K.length; i++)
			{
				boolean found = false;
				for (int scanComponentIndex = 0; !found && scanComponentIndex < mJPEG.comps_in_scan; scanComponentIndex++)
				{
					found = mSOSSegment.getACTable(scanComponentIndex) == i;
				}

				if (found)
				{
					aLog.println("    AC " + i);
					aLog.println("      k=" + mJPEG.arith_ac_K[i]);
				}
			}
		}

		return this;
	}
}
