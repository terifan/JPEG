package org.terifan.imageio.jpeg.exif;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.terifan.imageio.jpeg.JPEGConstants;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class JPEGExif
{
	private ArrayList<ExifEntry> mEntries;


	public JPEGExif()
	{
		mEntries = new ArrayList<>();
	}


	private JPEGExif(byte[] aImageData) throws IOException
	{
		this();

		Reader reader = new Reader(aImageData);

		if (reader.readInt16() != JPEGConstants.SOI)
		{
			throw new IOException("Not a JPEG file");
		}

		for (;;)
		{
			int code = reader.readInt16();

			if (code == JPEGConstants.EOI || code == JPEGConstants.SOS)
			{
				break;
			}

			int len = reader.readInt16();
			int pos = reader.byteBuffer.position();

			if (code == JPEGConstants.APP1)
			{
				parse2(reader, len - 2);
			}

			reader.byteBuffer.position(pos + len - 2);
		}
	}


	public ArrayList<ExifEntry> list()
	{
		return mEntries;
	}


	public ExifEntry get(ExifTag aTag)
	{
		for (ExifEntry entry : mEntries)
		{
			if (entry.getTag() == aTag)
			{
				return entry;
			}
		}

		return null;
	}


	public JPEGExif add(ExifEntry aTag)
	{
		mEntries.add(aTag);

		return this;
	}


	public <T> T value(ExifTag aTag, Class<T> aType)
	{
		for (ExifEntry entry : mEntries)
		{
			if (entry.getTag() == aTag)
			{
				return (T)entry.getValue();
			}
		}

		return null;
	}


	public String value(ExifTag aTag)
	{
		for (ExifEntry entry : mEntries)
		{
			if (entry.getTag() == aTag)
			{
				return entry.getValue().toString();
			}
		}

		return null;
	}


	public static JPEGExif extract(byte [] aImageData) throws IOException
	{
		return new JPEGExif(aImageData);
	}


	private void parse2(Reader aReader, int aLen) throws IOException
	{
		String header = "";

		while (aLen-- > 0)
		{
			int c = aReader.readInt8();
			if (c == 0)
			{
				break;
			}
			header += (char)c;
		}

		if (header.equals("Exif"))
		{
			if (aReader.readInt8() != 0)
			{
				throw new IOException("Bad TIFF header data");
			}

			byte[] exif = new byte[aLen - 1];

			aReader.byteBuffer.get(exif);

			parseImpl(exif);
		}
	}


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
			if (aBitStream.readInt8() != 0)
			{
				throw new IOException("Bad TIFF header data");
			}

			byte[] exif = new byte[len - 1];

			for (int i = 0; i < exif.length; i++)
			{
				exif[i] = (byte)aBitStream.readInt8();

//				System.out.printf("%02x ", exif[i]);
			}

			parseImpl(exif);
		}
		else
		{
			aBitStream.skipBytes(len);

			if (VERBOSE)
			{
				System.out.printf("  Unsupported APP1 segment content (%s)%n", header);
			}
		}
	}


	private void parseImpl(byte [] aExifData) throws IOException
	{
		Reader reader = new Reader(aExifData);

		switch (reader.readInt16())
		{
			case 0x4d4d: // "MM"
			case 0x6d6d: // "mm"
				reader.bigEndian = true;
				break;
			case 0x4949: // "II"
			case 0x6969: // "ii"
				reader.bigEndian = false;
				break;
			default:
				throw new IOException("Bad TIFF encoding header");
		}

		if (reader.readInt16() != 0x002a)
		{
			throw new IOException("Bad TIFF encoding header");
		}
		if (reader.readInt32() != 0x00000008)
		{
			throw new IOException("Bad TIFF encoding header");
		}

		for (;;)
		{
			int entryCount = reader.readInt16();

			for (int i = 0; i < entryCount; i++)
			{
				int tag = reader.readInt16();
				ExifFormat format = ExifFormat.values()[reader.readInt16() - 1];
				int length = reader.readInt32();
				int value = reader.readInt32();
				Object output = null;

				if (tag == ExifTag.PADDING.mCode) // ignore padding
				{
					continue;
				}

				int pos = reader.byteBuffer.position();

				switch (format)
				{
					case UBYTE:
						if (length <= 2)
						{
							output = "";
						}
						else if (length == 4)
						{
							output = "" + Character.reverseBytes((char)(value >> 16));
						}
						else
						{
							reader.byteBuffer.position(value);
							char [] dst = new char[length / 2 - 1];
							for (int j = 0; j < dst.length; j++)
							{
								dst[j] = Character.reverseBytes(reader.byteBuffer.getChar());
							}
							output = new String(dst);
						}
						break;
					case STRING:
						if (validRange(value, length, tag, reader))
						{
							byte [] dst = new byte[length - 1];
							reader.byteBuffer.position(value);
							reader.byteBuffer.get(dst);
							output = new String(dst);
						}
						break;
					case USHORT:
						output = (value >> 16) & 0xffff;
						break;
					case ULONG:
						output = value;
						break;
					case URATIONAL:
						if (validRange(value, length, tag, reader))
						{
							reader.byteBuffer.position(value);
							output = reader.readInt32() / (double)reader.readInt32();
						}
						break;
					case UNDEFINED:
						if (validRange(value, length, tag, reader))
						{
							output = new byte[length - 1];
							reader.byteBuffer.position(value);
							reader.byteBuffer.get((byte[])output);
						}
						break;
					default:
						if (VERBOSE)
						{
							System.out.printf("  Unsupported Exif tag: tag=%04X length=%-4d value=%-4d format=%s\n", tag, length, value, format);
						}
						output = null;
						break;
				}

				if (output != null)
				{
					mEntries.add(new ExifEntry(aExifData, format, tag, output));
				}

				reader.byteBuffer.position(pos);
			}

			int nextIFD = reader.readInt32();

			if (nextIFD == 0)
			{
				break;
			}

			reader.byteBuffer.position(nextIFD);
		}
	}


	public byte [] encode() throws IOException
	{
		ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(dataBuffer);

		ByteArrayOutputStream dirBuffer = new ByteArrayOutputStream();
		DataOutputStream dir = new DataOutputStream(dirBuffer);
		dir.write("Exif".getBytes());
		dir.writeShort(0x0000);
		dir.writeShort(0x4d4d);
		dir.writeShort(0x002a);
		dir.writeInt(0x00000008);
		dir.writeShort(mEntries.size());

		int dataOffset = 1024 - 6;

		for (ExifEntry entry : mEntries)
		{
			dir.writeShort(entry.getCode());
			dir.writeShort(entry.getFormat().ordinal() + 1);

			switch (entry.getFormat())
			{
				case ULONG:
				{
					dir.writeInt(0);
					dir.writeInt((Integer)entry.getValue());
					break;
				}
				case USHORT:
				{
					dir.writeInt(0);
					dir.writeInt((Integer)entry.getValue() << 16);
					break;
				}
				case UBYTE:
				{
					char[] s;
					if (entry.getValue() instanceof char[])
					{
						s = (char[])entry.getValue();
					}
					else
					{
						s = entry.getValue().toString().toCharArray();
					}

					if (s.length == 1)
					{
						dir.writeInt(4);
						dir.writeChar(Character.reverseBytes(s[0]));
						dir.writeChar(0);
					}
					else
					{
						dir.writeInt(2 * s.length + 2);
						dir.writeInt(dataOffset + data.size());
						for (int i = 0; i < s.length; i++)
						{
							data.writeChar(Character.reverseBytes(s[i])); // always little endian
						}
						data.writeChar(0);
					}
					break;
				}
				case URATIONAL:
				{
					dir.writeInt(1);
					dir.writeInt(dataOffset + data.size());

					double v = (Double)entry.getValue();
					data.writeInt((int)(v * 1000000));
					data.writeInt(1000000);
					break;
				}
				case STRING:
				{
					byte [] chunk = entry.getValue().toString().getBytes();

					dir.writeInt(chunk.length + 1);
					dir.writeInt(dataOffset + data.size());
					data.write(chunk);
					data.write(0);
					break;
				}
				case UNDEFINED:
				{
					byte [] chunk = (byte[])entry.getValue();

					dir.writeInt(chunk.length + 1);
					dir.writeInt(dataOffset + data.size());
					data.write(chunk);
					data.write(0);
					break;
				}
				default:
					throw new IllegalStateException("not implemented: " + entry);
			}
		}

		dir.writeShort(0);

		dir.write(new byte[dataOffset - dir.size() + 6]);

		dataBuffer.writeTo(dir);

		return dirBuffer.toByteArray();
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

			if (code == JPEGConstants.DQT && aNewMetaData != null && aNewMetaData.length > 0) // insert Exif data right before first quantization table
			{
				dos.writeShort(JPEGConstants.APP1);
				dos.writeShort(aNewMetaData.length + 2);
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

			if (code != JPEGConstants.APP1 && code != JPEGConstants.APP2 && code != JPEGConstants.APP13) // ignore app chunks containing metadata
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


	private boolean validRange(int aOffset, int aLength, int aTag, Reader aReader)
	{
		if (aOffset < 0 || aOffset + aLength >= aReader.byteBuffer.capacity())
		{
			if (VERBOSE)
			{
				System.out.printf("  ERROR: Buffer overflow found: attempt to read at offset %d +%s, capacity %d, tag \"%s\" (0x%04x)%n", aOffset, aLength, aReader.byteBuffer.capacity(), ExifTag.decode(aTag), aTag);
			}

			return false;
		}

		return true;
	}


	private static class Reader
	{
		boolean bigEndian = true;
		ByteBuffer byteBuffer;


		public Reader(byte [] aData)
		{
			byteBuffer = ByteBuffer.wrap(aData);
		}


		public int readInt8()
		{
			return byteBuffer.get() & 0xff;
		}


		public int readInt16()
		{
			if (bigEndian)
			{
				return byteBuffer.getShort() & 0xffff;
			}
			return Short.reverseBytes(byteBuffer.getShort()) & 0xffff;
		}


		public int readInt32()
		{
			if (bigEndian)
			{
				return byteBuffer.getInt();
			}
			return Integer.reverseBytes(byteBuffer.getInt());
		}
	}


	@Override
	public String toString()
	{
		return mEntries.toString();
	}
}
