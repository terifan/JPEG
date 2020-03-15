package org.terifan.imageio.jpeg;

import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class APP2Segment extends Segment
{
	public final static String ICC_PROFILE = "ICC_PROFILE";

	private JPEG mJPEG;
	private String mType;
	private int mVersion;


	public APP2Segment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
		mVersion = 0x0101;
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
	public APP2Segment decode(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		mType = aBitStream.readString();

		switch (mType)
		{
			case ICC_PROFILE:
				mVersion = aBitStream.readInt16(); // version

				byte[] buffer = new byte[length - 2 - ICC_PROFILE.length() - 1];
				aBitStream.read(buffer);

				mJPEG.mICCProfile = ICC_Profile.getInstance(new ByteArrayInputStream(buffer));
				break;
			default:
				aBitStream.skipBytes(length - mType.length() - 1);
				break;
		}

		return this;
	}


	@Override
	public APP2Segment encode(BitOutputStream aBitStream) throws IOException
	{
		if (mJPEG.mICCProfile != null)
		{
			byte[] data = mJPEG.mICCProfile.getData();

			aBitStream.writeInt16(SegmentMarker.APP2.CODE);
			aBitStream.writeInt16(2 + 12 + 2 + data.length);
			aBitStream.writeString(ICC_PROFILE);
			aBitStream.writeInt16(mVersion);
			aBitStream.write(data);
		}

		return this;
	}


	@Override
	public APP2Segment print(Log aLog) throws IOException
	{
		aLog.println("APP2 segment");
		switch (mType)
		{
			case ICC_PROFILE:
				aLog.println("ICC_PROFILE");
				break;
			default:
				aLog.println("Unsupported APP2 extension - %s", mType);
				break;
		}

		return this;
	}
}
