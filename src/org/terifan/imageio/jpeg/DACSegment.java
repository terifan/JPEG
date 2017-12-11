package org.terifan.imageio.jpeg;

import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_ARITH_TBLS;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


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
//				System.out.println("  arith_ac_K[" + (index - NUM_ARITH_TBLS) + "]=" + val);

				mJPEG.arith_ac_K[index - NUM_ARITH_TBLS] = val;
			}
			else // define DC table
			{
				mJPEG.arith_dc_U[index] = val >> 4;
				mJPEG.arith_dc_L[index] = val & 0x0F;

//				System.out.println("  arith_dc_L[" + index + "]=" + cinfo.arith_dc_L[index]);
//				System.out.println("  arith_dc_U[" + index + "]=" + cinfo.arith_dc_U[index]);

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
}
