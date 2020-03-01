package org.terifan.imageio.jpeg.exif;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.terifan.imageio.jpeg.JPEGConstants;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class JPEGExif
{
	private ArrayList<ExifTable> mTables;


	public JPEGExif()
	{
		mTables = new ArrayList<>();
	}


	public ArrayList<ExifTable> list()
	{
		return mTables;
	}


	public JPEGExif add(ExifTable aTable)
	{
		mTables.add(aTable);
		return this;
	}


	public JPEGExif remove(ExifTable aTable)
	{
		mTables.remove(aTable);
		return this;
	}


	public ExifTable get(int aTableIndex)
	{
		return mTables.get(aTableIndex);
	}


	public static JPEGExif extract(byte [] aImageData) throws IOException
	{
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(aImageData));

		if (dis.readShort() != (short)JPEGConstants.SOI)
		{
			throw new IOException("Not a JPEG file");
		}

		for (;;)
		{
			int code = dis.readShort();

			if (code == (short)JPEGConstants.EOI || code == (short)JPEGConstants.SOS)
			{
				break;
			}

			int len = 0xffff & dis.readShort();

			if (code == (short)JPEGConstants.APP1 && len > 6)
			{
				byte[] buf = new byte[6];
				dis.readFully(buf);

				if (Arrays.equals(buf, "Exif\u0000\u0000".getBytes()))
				{
					buf = new byte[len - 2 - 6];
					dis.readFully(buf);

					return new JPEGExif().decode(buf);
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


	public void read(BitInputStream aBitStream) throws IOException
	{
		int len = aBitStream.readInt16() - 2;

		read(aBitStream, len);
	}


	public void read(BitInputStream aBitStream, int aLength) throws IOException
	{
		String header = "";

		while (aLength-- > 0)
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
			if (aBitStream.readInt8() != 0)
			{
				throw new IOException("Bad TIFF header data");
			}

			byte[] exif = new byte[aLength - 1];

			for (int i = 0; i < exif.length; i++)
			{
				exif[i] = (byte)aBitStream.readInt8();
			}

			decode(exif);
		}
		else
		{
			aBitStream.skipBytes(aLength);

			if (VERBOSE)
			{
				System.out.printf("  Unsupported APP1 segment content (%s)%n", header);
			}
		}
	}


	public JPEGExif decode(byte [] aExifData) throws IOException
	{
		Reader reader = new Reader(aExifData);

//		Debug.hexDump(aExifData);

		switch (reader.readInt16())
		{
			case 0x4d4d: // "MM"
			case 0x6d6d: // "mm"
				reader.setBigEndian(true);
				break;
			case 0x4949: // "II"
			case 0x6969: // "ii"
				reader.setBigEndian(false);
				break;
			default:
				throw new IOException("Bad TIFF encoding header");
		}

		if (reader.readInt16() != 0x002a)
		{
			throw new IOException("Bad TIFF encoding header");
		}

		reader.position(reader.readInt32());

		for (;;)
		{
			ExifTable table = new ExifTable();
			mTables.add(table);

			int nextIFD;

			try
			{
				table.read(aExifData, reader);

				nextIFD = reader.readInt32();
			}
			catch (DataAccessException e)
			{
				break;
			}

			if (nextIFD <= 0 || nextIFD >= reader.capacity())
			{
				break;
			}

			reader.position(nextIFD);
		}

		return this;
	}


	public byte [] encode() throws IOException
	{
		Writer writer = new Writer();
		writer.setBigEndian(!true);
		writer.write(new byte[8]);

		ArrayList<Integer> pointerOffsets = new ArrayList<>();
		pointerOffsets.add(4);

		for (ExifTable table : mTables)
		{
			writer.position(writer.size());

			table.write(writer);

			pointerOffsets.add(table.getNextPointerOffset());
		}

		writer.position(0);
		writer.writeInt16(writer.isBigEndian() ? 0x4d4d : 0x4949);
		writer.writeInt16(0x002a);

		for (int i = 0; i < mTables.size(); i++)
		{
			writer.position(pointerOffsets.get(i));
			writer.writeInt32(mTables.get(i).mContentOffset);

//			System.out.println(pointerOffsets.get(i) + " -> " + mTables.get(i).mContentOffset);
		}

		return writer.toByteArray();
	}


	/**
	 * Replace first APP1 segment with the provided data.
	 *
	 * @param aImageData
	 *   the JPEG image
	 * @param aNewMetaData
	 *   the new EXIF data or null
	 * @return
	 *   the new JPEG image
	 */
	public static byte [] replace(byte [] aImageData, byte [] aNewMetaData) throws IOException
	{
		ByteArrayOutputStream dosBuffer = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(dosBuffer);

		ByteBuffer dis = ByteBuffer.wrap(aImageData);

		if (dis.getShort() != (short)JPEGConstants.SOI)
		{
			throw new IOException("Not a JPEG file");
		}

		dos.writeShort(JPEGConstants.SOI);

		for (;;)
		{
			int pos = dis.position();
			int code = dis.getShort() & 0xffff;

			if (code != JPEGConstants.APP0 && aNewMetaData != null && aNewMetaData.length > 0)
			{
				dos.writeShort(JPEGConstants.APP1);
				dos.writeShort(aNewMetaData.length + 2 + 6);
				dos.write("Exif".getBytes());
				dos.writeShort(0);
				dos.write(aNewMetaData);

				aNewMetaData = null;
			}

			if (code == JPEGConstants.SOS || code == JPEGConstants.EOI) // copy remaining bytes and return
			{
				byte [] tmp = new byte[aImageData.length - dis.position()];
				dis.get(tmp, 0, tmp.length);

				dos.writeShort(code);
				dos.write(tmp);

				break;
			}

			int len = dis.getShort() & 0xffff;

			if (code != JPEGConstants.APP1) // ignore app chunks containing metadata
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


	public void print()
	{
		for (ExifTable table : mTables)
		{
			table.print("");
		}
	}


	public ExifTable getThumbnailTable(int aIndex)
	{
		for (ExifTable table : mTables)
		{
			if (table.getThumbData() != null && aIndex-- == 0)
			{
				return table;
			}
		}

		return null;
	}


	public byte[] getThumbnailImage(int aIndex)
	{
		for (ExifTable table : mTables)
		{
			if (table.getThumbData() != null && aIndex-- == 0)
			{
				return table.getThumbData();
			}
		}

		return null;
	}


	@Override
	public String toString()
	{
		return mTables.toString();
	}
}
