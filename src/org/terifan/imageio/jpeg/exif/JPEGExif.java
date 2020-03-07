package org.terifan.imageio.jpeg.exif;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.SegmentMarker;


public class JPEGExif
{
	private JPEGExif()
	{
	}


	/**
	 * Extract Exif data from a JPEG image.
	 */
	public static Exif extract(byte [] aImageData) throws IOException
	{
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(aImageData));

		if (dis.readShort() != (short)SegmentMarker.SOI.CODE)
		{
			throw new IOException("Not a JPEG file");
		}

		for (;;)
		{
			int code = 0xffff & dis.readShort();

			if (code == SegmentMarker.EOI.CODE || code == SegmentMarker.SOS.CODE)
			{
				break;
			}

			int len = 0xffff & dis.readShort();

			if (code == SegmentMarker.APP1.CODE && len > 6)
			{
				byte[] buf = new byte[6];
				dis.readFully(buf);

				if (Arrays.equals(buf, "Exif\u0000\u0000".getBytes()))
				{
					buf = new byte[len - 2 - 6];
					dis.readFully(buf);

					return new Exif().decode(buf);
				}
				else
				{
					dis.skip(len - 2 - 6);
				}
			}
			else
			{
				dis.skip(len - 2);
			}
		}

		return null;
	}


	/**
	 * Replace the Exif data in a JPEG image.
	 *
	 * @param aImageData
	 *   the JPEG image data
	 * @param aExifData
	 *   the new EXIF data or null to remove any existing data
	 * @return
	 *   the new JPEG image data
	 */
	public static byte [] replace(byte [] aImageData, byte [] aExifData) throws IOException
	{
		ByteArrayOutputStream dosBuffer = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(dosBuffer);

		ByteBuffer dis = ByteBuffer.wrap(aImageData);

		if (dis.getShort() != (short)SegmentMarker.SOI.CODE)
		{
			throw new IOException("Not a JPEG file");
		}

		dos.writeShort(SegmentMarker.SOI.CODE);

		for (;;)
		{
			int pos = dis.position();
			int code = dis.getShort() & 0xffff;

			if (code != SegmentMarker.APP0.CODE && aExifData != null && aExifData.length > 0)
			{
				dos.writeShort(SegmentMarker.APP1.CODE);
				dos.writeShort(aExifData.length + 2 + 6);
				dos.write("Exif".getBytes());
				dos.writeShort(0);
				dos.write(aExifData);

				aExifData = null;
			}

			if (code == SegmentMarker.SOS.CODE || code == SegmentMarker.EOI.CODE) // copy remaining bytes and return
			{
				byte [] tmp = new byte[aImageData.length - dis.position()];
				dis.get(tmp, 0, tmp.length);

				dos.writeShort(code);
				dos.write(tmp);

				break;
			}

			int len = dis.getShort() & 0xffff;

			if (code != SegmentMarker.APP1.CODE) // ignore app chunks containing metadata
			{
				byte [] tmp = new byte[len + 2];
				dis.position(pos);
				dis.get(tmp, 0, tmp.length);

				dos.write(tmp);
			}

			dis.position(pos + len + 2);
		}

		return dosBuffer.toByteArray();
	}
}
