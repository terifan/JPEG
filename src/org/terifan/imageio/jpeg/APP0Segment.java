package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class APP0Segment extends Segment
{
	private boolean mExtension;
	private int mThumbnailWidth;
	private int mThumbnailHeight;
	private byte[] mThumbnailData;
	private int mThumbnailFormat;

	public int mDensitiesUnits;
	public int mDensityX;
	public int mDensityY;


	public APP0Segment()
	{
		mDensitiesUnits = 1;
		mDensityX = 72;
		mDensityY = 72;

		mThumbnailData = new byte[0];
	}


	public APP0Segment decode(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16();
		String type = aBitStream.readString();

		switch (type)
		{
			case "JFIF":
				int version = aBitStream.readInt16();

				if ((version >> 8) != 1)
				{
					throw new IOException("Error in JPEG stream; unsupported version: " + (version >> 8) + "." + (version & 255));
				}

				mDensitiesUnits = aBitStream.readInt8();
				mDensityX = aBitStream.readInt16();
				mDensityY = aBitStream.readInt16();

				mThumbnailWidth = aBitStream.readInt8();
				mThumbnailHeight = aBitStream.readInt8();
				mThumbnailData = new byte[mThumbnailWidth * mThumbnailHeight * 3];

				aBitStream.read(mThumbnailData);

				if (length != 16 + mThumbnailData.length)
				{
					throw new IOException("Error in JPEG stream; illegal APP0 segment size.");
				}

				break;
			case "JFXX":
				mExtension = true;

				mThumbnailFormat = aBitStream.readInt8();

				mThumbnailData = new byte[length - 2 - 5 - 1];

				aBitStream.read(mThumbnailData);

				break;
			default:
				throw new IOException("Unsupported APP0 extension: " + type);
		}

		return this;
	}


	public APP0Segment encode(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt16(SegmentMarker.APP0.CODE);

		if (mExtension)
		{
			aBitStream.writeInt16(2 + 5 + 1 + mThumbnailData.length);
			aBitStream.writeString("JFXX");
			aBitStream.writeInt8(mThumbnailFormat);
			aBitStream.write(mThumbnailData);
		}
		else
		{
			aBitStream.writeInt16(2 + 5 + 2 + 1 + 2 + 2 + 1 + 1 + mThumbnailData.length);
			aBitStream.writeString("JFIF");
			aBitStream.writeInt16(0x0102); // version
			aBitStream.writeInt8(mDensitiesUnits);
			aBitStream.writeInt16(mDensityX);
			aBitStream.writeInt16(mDensityY);
			aBitStream.writeInt8(mThumbnailWidth); // thumbnail width
			aBitStream.writeInt8(mThumbnailHeight); // thumbnail height
			aBitStream.write(mThumbnailData);
		}

		return this;
	}


	@Override
	public APP0Segment print(JPEG aJPEG, Log aLog) throws IOException
	{
		if (mExtension)
		{
			aLog.println("APP0 segment");
			aLog.println("  JFXX");
			if (mThumbnailData.length > 0)
			{
				aLog.println("  thumbnail");
				aLog.println("  width=%d, height=%d, data=%d bytes", mThumbnailWidth, mThumbnailHeight, mThumbnailData.length);
			}
		}
		else
		{
			aLog.println("APP0 segment");
			aLog.println("  JFIF");
			if (mThumbnailData.length > 0)
			{
				aLog.println("  thumbnail");
				aLog.println("  format=%d, data=%d bytes", mThumbnailFormat, mThumbnailData.length);
			}
		}

		return this;
	}
}
