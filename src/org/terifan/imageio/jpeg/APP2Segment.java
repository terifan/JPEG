package org.terifan.imageio.jpeg;

import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class APP2Segment
{
	private JPEG mJPEG;


	public APP2Segment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public void read(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16();
		int offset = aBitStream.getStreamOffset();

		StringBuilder type = new StringBuilder();
		for (int c; (c = aBitStream.readInt8()) != 0;)
		{
			type.append((char)c);
		}

		switch (type.toString())
		{
			case "ICC_PROFILE":
//				int profileSize = aBitStream.readInt32();
//				int preferredCMMType = aBitStream.readInt32();
//				int profileVersionNumber = aBitStream.readInt32();
//				int profileDeviceClass = aBitStream.readInt32();
//				int colourSpace = aBitStream.readInt32();
//				int profileConnectionSpace = aBitStream.readInt32();
//				int dateAndTime1 = aBitStream.readInt32();
//				int dateAndTime2 = aBitStream.readInt32();
//				int dateAndTime3 = aBitStream.readInt32();
//				int profileFileSignature = aBitStream.readInt32();
//				int primaryPlatformSignature = aBitStream.readInt32();
//				int profileFlags = aBitStream.readInt32();
//				int deviceManufacturer = aBitStream.readInt32();
//				int deviceModel = aBitStream.readInt32();
//				int deviceAttributes1 = aBitStream.readInt32();
//				int deviceAttributes2 = aBitStream.readInt32();
//				int renderingIntent = aBitStream.readInt32();
//				int XYZValues1 = aBitStream.readInt32();
//				int XYZValues2 = aBitStream.readInt32();
//				int XYZValues3 = aBitStream.readInt32();
//				int profileCreatorSignature = aBitStream.readInt32();
//				int profileID1 = aBitStream.readInt32();
//				int profileID2 = aBitStream.readInt32();
//				int profileID3 = aBitStream.readInt32();
//				int profileID4 = aBitStream.readInt32();
//				aBitStream.skipBytes(28); // reserved
//				
//				aBitStream.readInt16(); // unknown
//				
//				int count = aBitStream.readInt32();
//
//				for (int i = 0; i < count; i++)
//				{
//					int tag = aBitStream.readInt32();
//					int pos = aBitStream.readInt32();
//					int len = aBitStream.readInt32();
//				}

				aBitStream.skipBytes(2);

				byte[] data = aBitStream.readBytes(new byte[offset + length - aBitStream.getStreamOffset() - 2]);

				mJPEG.mICCProfile = ICC_Profile.getInstance(data);
				
				break;
			default:
				throw new IOException("Unsupported APP2 extension: " + type);
		}

//		aBitStream.skipBytes(offset + length - aBitStream.getStreamOffset() - 2);
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
