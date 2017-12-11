package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class APP14Segment 
{
	private ColorSpace.ColorSpaceType mColorSpace;
	private JPEG mJPEG;


	public APP14Segment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public void read(BitInputStream aBitStream) throws IOException
	{
		int offset = aBitStream.getStreamOffset();
		int length = aBitStream.readInt16();

		if (aBitStream.readInt8() == 'A' && aBitStream.readInt8() == 'd' && aBitStream.readInt8() == 'o' && aBitStream.readInt8() == 'b' && aBitStream.readInt8() == 'e')
		{
			if (aBitStream.readInt8() != 100)
			{
				aBitStream.skipBytes(1); //flags 0
				aBitStream.skipBytes(1); //flags 1

				switch (aBitStream.readInt8())
				{
					case 1:
						mColorSpace = ColorSpace.ColorSpaceType.YCBCR;
						break;
					case 2:
						mColorSpace = ColorSpace.ColorSpaceType.YCCK;
						break;
					default:
						mColorSpace = ColorSpace.ColorSpaceType.RGB;
						break;
				}
			}
		}

		if (aBitStream.getStreamOffset() != offset + length)
		{
			throw new IOException("Expected offset " + (offset + length) + ", actual " + aBitStream.getStreamOffset());
		}
	}
}
