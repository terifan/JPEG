package org.terifan.imageio.jpeg;

import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class APP0Segment
{
	private JPEG mJPEG;


	public APP0Segment(JPEG aJPEG)
	{
		mJPEG = aJPEG;

		mJPEG.mDensitiesUnits = 1;
		mJPEG.mDensityX = 100;
		mJPEG.mDensityY = 100;
	}


	public void read(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16();

		StringBuilder type = new StringBuilder();
		for (int c; (c = aBitStream.readInt8()) != 0;)
		{
			type.append((char)c);
		}

		switch (type.toString())
		{
			case "JFIF":
				int version = aBitStream.readInt16();

				if ((version >> 8) != 1)
				{
					throw new IOException("Error in JPEG stream; unsupported version: " + (version >> 8) + "." + (version & 255));
				}

				mJPEG.mDensitiesUnits = aBitStream.readInt8();
				mJPEG.mDensityX = aBitStream.readInt16();
				mJPEG.mDensityY = aBitStream.readInt16();

				int thumbnailSize = aBitStream.readInt8() * aBitStream.readInt8() * 3; // thumbnailWidth, thumbnailHeight

				aBitStream.skipBytes(thumbnailSize); // uncompressed 24-bit thumbnail raster

				if (VERBOSE)
				{
					if (thumbnailSize > 0)
					{
						System.out.println("Ignoring thumbnail " + thumbnailSize + " bytes");
					}
				}

				if (length != 16 + thumbnailSize)
				{
					throw new IOException("Error in JPEG stream; illegal APP0 segment size.");
				}

				break;
			case "JFXX":
				int extensionCode = aBitStream.readInt8();
				switch (extensionCode)
				{
					case 0x10: // jpeg encoded
					case 0x11: // 8-bit palette
					case 0x13: // 24-bit RGB
				}

				aBitStream.skipBytes(length - 8);

				if (VERBOSE)
				{
					if (length - 8 > 0)
					{
						System.out.println("Ignoring thumbnail " + (length - 8) + " bytes");
					}
				}
				break;
			default:
				throw new IOException("Unsupported APP0 extension: " + type);
		}
	}


	public void write(BitOutputStream aBitStream) throws IOException
	{
		aBitStream.writeInt16(JPEGConstants.APP0);
		aBitStream.writeInt16(16);
		aBitStream.write("JFIF".getBytes(), 0, 4);
		aBitStream.writeInt8(0);
		aBitStream.writeInt16(0x0101); // version
		aBitStream.writeInt8(mJPEG.mDensitiesUnits);
		aBitStream.writeInt16(mJPEG.mDensityX);
		aBitStream.writeInt16(mJPEG.mDensityY);
		aBitStream.writeInt8(0); // thumbnail width
		aBitStream.writeInt8(0); // thumbnail height
	}
}
