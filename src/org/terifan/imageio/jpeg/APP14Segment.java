package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class APP14Segment
{
	private ColorSpaceType mColorSpace;
	private JPEG mJPEG;


	public APP14Segment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public void read(BitInputStream aBitStream) throws IOException
	{
		int offset = aBitStream.getStreamOffset();
		int length = aBitStream.readInt16();

		if (aBitStream.readInt8() == 'A' && aBitStream.readInt8() == 'd' && aBitStream.readInt8() == 'o' && aBitStream.readInt8() == 'b' && aBitStream.readInt8() == 'e' && aBitStream.readInt8() == 0)
		{
			int version = aBitStream.readInt8();

			if (version == 100)
			{
				aBitStream.skipBytes(2); //flags 0
				aBitStream.skipBytes(2); //flags 1

				switch (aBitStream.readInt8())
				{
					case 1:
						mColorSpace = ColorSpaceType.YCBCR;
						break;
					case 2:
						mColorSpace = ColorSpaceType.YCCK;
						break;
					default:
						mColorSpace = ColorSpaceType.RGB;
						break;
				}
			}
		}

		int remaining = offset + length - aBitStream.getStreamOffset();

		if (remaining < 0)
		{
			throw new IOException("Expected offset " + (offset + length) + ", actual " + aBitStream.getStreamOffset());
		}
		else if (remaining > 0)
		{
			aBitStream.skipBytes(remaining);
		}
	}
}
