package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.JPEGBitInputStream;
import org.terifan.imageio.jpeg.encoder.JPEGBitOutputStream;


public class APP14Segment extends Segment
{
	private int mVersion;
	private int mFlags0;
	private int mFlags1;
	private int mTransform;


	public APP14Segment()
	{
	}


	public int getTransform()
	{
		return mTransform;
	}


	public APP14Segment decode(JPEGBitInputStream aBitStream) throws IOException
	{
		int offset = aBitStream.getStreamOffset();
		int length = aBitStream.readInt16();

		if (aBitStream.readInt8() == 'A' && aBitStream.readInt8() == 'd' && aBitStream.readInt8() == 'o' && aBitStream.readInt8() == 'b' && aBitStream.readInt8() == 'e')
		{
			mVersion = aBitStream.readInt16();
			mFlags0 = aBitStream.readInt16();
			mFlags1 = aBitStream.readInt16();
			mTransform = aBitStream.readInt8();
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

		return this;
	}


	@Override
	public APP14Segment print(JPEG aJPEG, Log aLog) throws IOException
	{
		aLog.println("APP14 segment");
		aLog.println("  Adobe");
//		aLog.println("  Color space %d", aJPEG.mColorSpace);

		return this;
	}
}
