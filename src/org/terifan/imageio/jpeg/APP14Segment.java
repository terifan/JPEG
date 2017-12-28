package org.terifan.imageio.jpeg;

import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class APP14Segment
{
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
			int version = aBitStream.readInt16();
			int flags0 = aBitStream.readInt16();
			int flags1 = aBitStream.readInt16();
			int transform = aBitStream.readInt8();
			
			switch (transform)
			{
				case 1:
					mJPEG.mColorSpace = ColorSpaceType.YCBCR;
					break;
				case 2:
					mJPEG.mColorSpace = ColorSpaceType.YCCK;
					break;
				default:
					mJPEG.mColorSpace = ColorSpaceType.RGB; // 3-channel images are assumed to be RGB, 4-channel images are assumed to be CMYK
					break;
			}

			mJPEG.saw_Adobe_marker = true;
		}

		int remaining = offset + length - aBitStream.getStreamOffset();

		if (VERBOSE)
		{
			System.out.println("Adobe APP14 marker segment");
			System.out.println("  Color space " + mJPEG.mColorSpace);
		}

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
