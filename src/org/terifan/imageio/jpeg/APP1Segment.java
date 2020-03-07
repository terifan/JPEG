package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;
import org.terifan.imageio.jpeg.exif.Exif;


public class APP1Segment implements Segment
{
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


	@Override
	public void read(BitInputStream aBitStream) throws IOException
	{
		int len = aBitStream.readInt16() - 2;

		String header = "";

		while (len-- > 0)
		{
			int c = aBitStream.readInt8();
			if (c == 0)
			{
				break;
			}
			header += (char)c;
		}

		if (header.equals("Exif"))
		{
			len--;
			if (aBitStream.readInt8() != 0)
			{
				throw new IOException("Bad TIFF header data");
			}

			byte[] exif = new byte[len - 1];

			for (int i = 0; i < exif.length; i++, len--)
			{
				exif[i] = (byte)aBitStream.readInt8();
			}

			mExif = new Exif().decode(exif);
		}

		aBitStream.skipBytes(len);

		if (JPEGConstants.VERBOSE)
		{
			System.out.printf("  Unsupported APP1 segment content (%s)%n", header);
		}
	}


	@Override
	public void write(BitOutputStream aBitStream) throws IOException
	{
	}
}
