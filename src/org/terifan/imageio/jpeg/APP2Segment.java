package org.terifan.imageio.jpeg;

import java.awt.color.ICC_Profile;
import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class APP2Segment implements Segment
{
	public final static String ICC_PROFILE = "ICC_PROFILE";
	public final static String MPF = "MPF";

	private JPEG mJPEG;
	private String mType;


	public APP2Segment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	@Override
	public void read(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16();
		int offset = aBitStream.getStreamOffset();

		mType = aBitStream.readString();

		switch (mType)
		{
			case ICC_PROFILE:
				aBitStream.skipBytes(2); // version
				mJPEG.mICCProfile = ICC_Profile.getInstance(aBitStream);
				break;
			case MPF:
				// unsupported
				aBitStream.skipBytes(length - 2 - aBitStream.getStreamOffset() + offset);
				break;
			default:
				throw new IOException("Unsupported APP2 extension: " + mType);
		}

		if (aBitStream.getStreamOffset() != offset + length - 2)
		{
			throw new IOException("Unrecognized data in APP2 segment");
		}
	}


	public String getType()
	{
		return mType;
	}


	public APP2Segment setType(String aType)
	{
		mType = aType;
		return this;
	}


	@Override
	public void write(BitOutputStream aBitStream) throws IOException
	{
		switch (mType)
		{
			case ICC_PROFILE:
				byte[] data = mJPEG.mICCProfile.getData();

				aBitStream.writeInt16(JPEGConstants.APP2);
				aBitStream.writeInt16(2 + 12 + 2 + data.length);
				aBitStream.writeString(mType);
				aBitStream.writeInt16(0x0101); // version
				aBitStream.write(data);
				break;
			default:
				throw new IOException("Unsupported APP2 extension: " + mType);
		}
	}
}
