package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;
import org.terifan.imageio.jpeg.exif.Exif;


public class APP1Segment extends Segment
{
	private final static String EXIF = "Exif";

	private JPEG mJPEG;
	private Exif mExif;


	public APP1Segment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public Exif getExif()
	{
		return mExif;
	}


	public void setExif(Exif aExif)
	{
		mExif = aExif;
	}


	@Override
	public APP1Segment decode(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		String header = aBitStream.readString();
		length -= header.length() + 1;

		switch (header)
		{
			case EXIF:
				if (aBitStream.readInt8() != 0)
				{
					throw new IOException("Bad TIFF header data");
				}

				length--;
				byte[] exif = new byte[length];

				for (int i = 0; i < exif.length; i++, length--)
				{
					exif[i] = (byte)aBitStream.readInt8();
				}

				mExif = new Exif().decode(exif);
				break;
			default:
				System.out.printf("  Ignoring unsupported APP1 segment content (%s)%n", header);
				aBitStream.skipBytes(length);
				break;
		}

		return this;
	}


	@Override
	public APP1Segment encode(BitOutputStream aBitStream) throws IOException
	{
		if (mExif != null)
		{
			byte[] data = mExif.encode();

			aBitStream.writeInt16(2 + 5 + data.length);
			aBitStream.writeString("Exif");
			aBitStream.write(data);
		}

		return this;
	}


	@Override
	public APP1Segment print(Log aLog) throws IOException
	{
		if (mExif != null)
		{
			aLog.println("APP1 segment");
			aLog.println("  EXIF");

			if (aLog.isDetailed())
			{
				mExif.print("    ", aLog);
			}
		}

		return this;
	}
}
