package org.terifan.imageio.jpeg;

import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DACSegment
{
	private JPEG mJPEG;


	public DACSegment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public void read(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		System.out.println("DACMarkerSegment");

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
				System.out.println("  arith_ac_K[" + index + "]=" + val);

				mJPEG.arith_ac_K[index - NUM_ARITH_TBLS] = val;
			}
			else // define DC table
			{
				mJPEG.arith_dc_U[index] = val >> 4;
				mJPEG.arith_dc_L[index] = val & 0x0F;

				System.out.println("  arith_dc_L[" + index + "]=" + mJPEG.arith_dc_L[index]);
				System.out.println("  arith_dc_U[" + index + "]=" + mJPEG.arith_dc_U[index]);

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
	}


	public void write(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt16(JPEGConstants.DAC);
		aBitStream.writeInt16(2 + 2 * (mJPEG.arith_dc_U.length + mJPEG.arith_ac_K.length));

		for (int i = 0; i < mJPEG.arith_dc_U.length; i++)
		{
			aBitStream.writeInt8(i);
			aBitStream.writeInt8((mJPEG.arith_dc_U[i] << 4) + mJPEG.arith_dc_L[i]);
		}
		for (int i = 0; i < mJPEG.arith_ac_K.length; i++)
		{
			aBitStream.writeInt8(NUM_ARITH_TBLS + i);
			aBitStream.writeInt8(mJPEG.arith_ac_K[i]);
		}
	}
}
