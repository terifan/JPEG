package org.terifan.imageio.jpeg.exif;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;


public class JPEGExif
{
	private static final int EXIF_HEADER = 0x45786966;


	private JPEGExif()
	{
	}


	public static Exif extract(byte [] aImageData) throws IOException
	{
		Reader reader = new Reader(aImageData);

		ArrayList<ExifEntry> entries = new ArrayList<>();

		if (reader.getShort() != 0xffd8)
		{
			throw new IOException("Not a JPEG file");
		}

		for (;;)
		{
			int code = reader.getShort() & 0xffff;

			if (code == 0xFFD9 || code == 0xFFDA)
			{
				break;
			}

			int len = reader.getShort() & 0xffff;
			int pos = reader.byteBuffer.position();

			//			System.out.println(pos+"\t"+len+"\t"+Integer.toHexString(code));

			if (code == 0xFFE1)
			{
				if (reader.getInt() == EXIF_HEADER)
				{
					if (reader.getShort() != 0)
					{
						throw new IOException("Bad TIFF header data");
					}

					byte [] exif = new byte[len - 6];
					reader.byteBuffer.get(exif, 0, len - 6);

					extractImpl(exif, entries);
				}
			}

			reader.byteBuffer.position(pos + len - 2);
		}

		return new Exif(entries);
	}


	private static void extractImpl(byte [] aExifData, ArrayList<ExifEntry> oEntries) throws IOException
	{
		Reader reader = new Reader(aExifData);

		switch (reader.getShort())
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

		if (reader.getShort() != 0x002a)
		{
			throw new IOException("Bad TIFF encoding header");
		}
		if (reader.getInt() != 0x00000008)
		{
			throw new IOException("Bad TIFF encoding header");
		}

		for (;;)
		{
			int entryCount = reader.getShort();

			for (int i = 0; i < entryCount; i++)
			{
				int tag = reader.getShort();
				ExifFormat format = ExifFormat.values()[reader.getShort() - 1];
				int length = reader.getInt();
				int value = reader.getInt();
				Object output;

//				System.out.printf("tag=%04X length=%-4d value=%-4d format=%s\n", tag, length, value, format);

				if (tag == ExifTag.PADDING.mCode) // ignore padding
				{
					continue;
				}

				int pos = reader.byteBuffer.position();

				switch (format)
				{
					case UBYTE:
					{
						if (length == 4)
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
					}
					case STRING:
					{
						byte [] dst = new byte[length - 1];
						reader.byteBuffer.position(value);
						reader.byteBuffer.get(dst);
						output = new String(dst);
						break;
					}
					case USHORT:
						output = (value >> 16) & 0xffff;
						break;
					case ULONG:
						output = value;
						break;
					case URATIONAL:
						reader.byteBuffer.position(value);
						output = reader.getInt() / (double)reader.getInt();
						break;
					case UNDEFINED:
						output = new byte[length - 1];
						reader.byteBuffer.position(value);
						reader.byteBuffer.get((byte[])output);
						break;
					default:
						System.out.printf("tag=%04X length=%-4d value=%-4d format=%s\n", tag, length, value, format);
						output = null;
						break;
				}

				oEntries.add(new ExifEntry(aExifData, format, tag, output));

				reader.byteBuffer.position(pos);
			}

			int nextIFD = reader.getInt();

			if (nextIFD == 0)
			{
				break;
			}

			reader.byteBuffer.position(nextIFD);
		}
	}


	private static class Reader
	{
		boolean bigEndian = true;
		ByteBuffer byteBuffer;


		public Reader(byte [] aData)
		{
			byteBuffer = ByteBuffer.wrap(aData);
		}


		public int getShort()
		{
			if (bigEndian)
			{
				return byteBuffer.getShort() & 0xffff;
			}
			return Short.reverseBytes(byteBuffer.getShort()) & 0xffff;
		}


		public int getInt()
		{
			if (bigEndian)
			{
				return byteBuffer.getInt();
			}
			return Integer.reverseBytes(byteBuffer.getInt());
		}
	}


	public static byte [] replace(byte [] aImageData, byte [] aNewMetaData) throws IOException
	{
		ByteArrayOutputStream dosBuffer = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(dosBuffer);

		ByteBuffer dis = ByteBuffer.wrap(aImageData);

		if (dis.getShort() != (short)0xFFD8)
		{
			throw new IOException("Not a JPEG file");
		}

		dos.writeShort(0xFFD8);

		for (;;)
		{
			int pos = dis.position();
			int code = dis.getShort() & 0xffff;

			if (code == 0xFFDB && aNewMetaData != null && aNewMetaData.length > 0) // insert Exif data
			{
				dos.writeShort(0xFFE1);
				dos.writeShort(aNewMetaData.length + 2);
				dos.write(aNewMetaData);

				aNewMetaData = null;
			}

			if (code == 0xFFDA || code == 0xFFD9) // copy remaining bytes and return
			{
				byte [] tmp = new byte[aImageData.length - dis.position()];
				dis.get(tmp, 0, tmp.length);

				dos.writeShort(code);
				dos.write(tmp);

				break;
			}

			int len = dis.getShort() & 0xffff;

			if (code != 0xFFE1 && code != 0xFFED && code != 0xFFE2) // ignore app chunks containing metadata
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


	public static byte [] encode(ArrayList<ExifEntry> aEntries) throws IOException
	{
		ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(dataBuffer);

		ByteArrayOutputStream dirBuffer = new ByteArrayOutputStream();
		DataOutputStream dir = new DataOutputStream(dirBuffer);
		dir.writeInt(EXIF_HEADER); // "Exif"
		dir.writeShort(0x0000);
		dir.writeShort(0x4d4d);
		dir.writeShort(0x002a);
		dir.writeInt(0x00000008);
		dir.writeShort(aEntries.size());

		int dataOffset = 1024 - 6;

		for (ExifEntry entry : aEntries)
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
}
