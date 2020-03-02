package org.terifan.imageio.jpeg.exif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/**
 *
 * 16 entry count
 * for each entry
 * {
 *    16 tag
 *    16 format
 *    32 length
 *    32 value
 * }
 * 32 next pointer
 * ?? entry data
 *
 */
public class ExifTable
{
	private final static boolean VERBOSE = false;

	private ArrayList<ExifEntry> mEntries;
	private ArrayList<ExifTable> mTables;
	private int mNextPointerOffset;
	private byte[] mThumbData;

	int mContentOffset;
	int mContentLength;


	public ExifTable()
	{
		mTables = new ArrayList<>();
		mEntries = new ArrayList<>();
	}


	public ExifTable getTable(int aIndex)
	{
		return mTables.get(aIndex);
	}


	public void removeTable(int aIndex)
	{
		mTables.remove(aIndex);
	}


	public void addTable(ExifTable aTable)
	{
		mTables.add(aTable);
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


	public ExifTable add(ExifEntry aEntry)
	{
		for (ExifEntry entry : mEntries)
		{
			if (entry.getTag() == aEntry.getTag())
			{
				mEntries.remove(entry);
				break;
			}
		}
		mEntries.add(aEntry);
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


	public byte[] getThumbData()
	{
		return mThumbData;
	}


	public void setThumbData(byte[] aThumbData)
	{
		mThumbData = aThumbData;
	}


	@Override
	public String toString()
	{
		return mEntries.toString();
	}


	void read(byte[] aExifData, Reader aReader) throws IOException
	{
		int entryCount = aReader.readInt16();

		// Exif points to a JPEG chunk, abort.
		if (aReader.isBigEndian() && (entryCount & 0xff00) == 0xff00 || !aReader.isBigEndian() && (entryCount & 0x00ff) == 0x00ff)
		{
			return;
		}

		if (VERBOSE)
		{
			System.out.println("Entry count: " + entryCount);
		}

		int startOffset = aReader.position();

		for (int entryIndex = 0; entryIndex < entryCount; entryIndex++)
		{
			try
			{
				int tag = aReader.readInt16();
				int formatIndex = aReader.readInt16();
				int length = aReader.readInt32();
				int value = aReader.readInt32();

				if (tag == ExifTag.PADDING.CODE) // ignore padding
				{
					if (VERBOSE)
					{
						System.out.println("PADDING");
					}
					continue;
				}

				readEntry(formatIndex, length, value, aReader, tag, startOffset, entryIndex, aExifData);
			}
			catch (DataAccessException e)
			{
				if (VERBOSE)
				{
					System.out.printf("Ignoring bad record at position: %d%n", startOffset + 12 * entryIndex);
				}
			}

			aReader.position(startOffset + (entryIndex + 1) * 12);
		}
	}


	private void readEntry(int aFormatIndex, int aLength, int aValue, Reader aReader, int aTag, int aStartOffset, int aEntryIndex, byte[] aExifData) throws IOException
	{
		Object output = null;

		ExifFormat format = ExifFormat.values()[aFormatIndex];

		switch (format)
		{
			case UBYTE:
				if (aLength <= 2)
				{
					output = "";
				}
				else if (aLength == 4)
				{
					output = "" + aReader.readInt16LE();
				}
				else
				{
					aReader.position(aValue);
					char[] dst = new char[aLength / 2 - 1];
					for (int j = 0; j < dst.length; j++)
					{
						dst[j] = (char)(aReader.readInt8() + 256 * aReader.readInt8());
					}
					output = new String(dst).trim();
				}
				break;
			case ASCII:
				if (aLength <= 4)
				{
					output = new String(toBuffer(aValue, aLength, aReader.isBigEndian()));
				}
				else if (validRange(aValue, aLength, aTag, aReader))
				{
					output = readString(aReader, aValue, aLength);
				}
				break;
			case USHORT:
				output = aReader.isBigEndian() ? aValue >>> 16 : aValue & 0xffff;
				break;
			case ULONG:
				output = aValue;
				break;
			case URATIONAL:
			case RATIONAL:
				output = aValue / (double)aLength;
				break;
			case UNDEFINED:
				byte[] b = null;
				if (aLength <= 4)
				{
					b = toBuffer(aValue, aLength, aReader.isBigEndian());
				}
				else if (validRange(aValue, aLength, aTag, aReader))
				{
					b = readBuffer(aReader, aValue, aLength);
				}
				if (b != null && new String(b).matches("[\\x20-\\x7F\\x0c\\x0a\\x08]*"))
				{
					output = new String(b);
				}
				else
				{
					output = b;
				}
				break;
			default:
				if (VERBOSE)
				{
					System.out.printf("  Unsupported Exif tag: tag=%04X length=%-4d value=%-4d format=%s\n", aTag, aLength, aValue, format);
				}
				output = null;
				break;
		}

		if (VERBOSE)
		{
			System.out.printf("%4d %4d %04X %4d %4d %30s [%11s] = %s%n", aStartOffset + 12 * aEntryIndex, aReader.capacity(), aTag, aFormatIndex, aLength, ExifTag.valueOf(aTag), aValue, output);
		}

		if (output != null)
		{
			add(new ExifEntry(format, aTag, output));

			if (aTag == ExifTag.ExifOffset.CODE)
			{
				aReader.position(((Number)output).intValue());

				ExifTable table = new ExifTable();
				table.read(aExifData, aReader);
				mTables.add(table);
			}

			if (aTag == ExifTag.ThumbJpegIFOffset.CODE || aTag == ExifTag.ThumbJpegIFByteCount.CODE)
			{
				ExifEntry ofsEntry = get(ExifTag.ThumbJpegIFOffset);
				ExifEntry lenEntry = get(ExifTag.ThumbJpegIFByteCount);

				if (ofsEntry != null && lenEntry != null)
				{
					if (ofsEntry.getValue() instanceof byte[])
					{
						mThumbData = (byte[])ofsEntry.getValue();
					}
					else
					{
						int len = ((Number)lenEntry.getValue()).intValue();
						int ofs = ((Number)ofsEntry.getValue()).intValue();

						if (len > 0 && ofs + len <= aReader.capacity())
						{
							mThumbData = new byte[len];

							aReader.position(ofs);
							aReader.read(mThumbData);
						}
					}
				}
			}
		}
	}


	void write(Writer aWriter) throws IOException
	{
		for (int i = mEntries.size(); --i >= 0;)
		{
			int code = mEntries.get(i).getCode();
			if (code == ExifTag.ExifOffset.CODE || code == ExifTag.ThumbJpegIFOffset.CODE || code == ExifTag.ThumbJpegIFByteCount.CODE)
			{
				mEntries.remove(i);
			}
		}

		ExifEntry thumbOffsetEntry = null;

		if (mThumbData != null)
		{
			thumbOffsetEntry = new ExifEntry(ExifTag.ThumbJpegIFOffset, 0);
			mEntries.add(new ExifEntry(ExifTag.ThumbJpegIFByteCount, mThumbData.length));
			mEntries.add(thumbOffsetEntry);
		}

		mContentOffset = aWriter.position();

		int dataOffset = aWriter.position() + 2 + (mEntries.size() + mTables.size()) * 12 + 4;

		if (VERBOSE)
		{
			System.out.println("table (" + mEntries.size() + " + " + mTables.size() + ") " + aWriter.position() + " " + dataOffset);
		}

		Writer dataOutput = new Writer();

		aWriter.writeInt16(mEntries.size() + mTables.size());

		int thumbnailRecordOffset = -1;

		for (ExifEntry entry : mEntries)
		{
			if (entry == thumbOffsetEntry)
			{
				thumbnailRecordOffset = aWriter.position();
			}

			if (VERBOSE)
			{
				System.out.println("\t" + entry);
			}

			aWriter.writeInt16(entry.getCode());
			aWriter.writeInt16(entry.getFormat().ordinal());

			switch (entry.getFormat())
			{
				case ULONG:
				{
					aWriter.writeInt32(0);
					aWriter.writeInt32((Integer)entry.getValue());
					break;
				}
				case USHORT:
				{
					aWriter.writeInt32(1);
					aWriter.writeInt32(aWriter.isBigEndian() ? ((Integer)entry.getValue() << 16) : ((Integer)entry.getValue()));
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
						aWriter.writeInt32(4);
						aWriter.writeInt16LE(s[0]);
						aWriter.writeInt16(0);
					}
					else
					{
						aWriter.writeInt32(2 * s.length + 2);
						aWriter.writeInt32(dataOffset + dataOutput.size());
						for (int i = 0; i < s.length; i++)
						{
							dataOutput.writeInt16LE(s[i]);
						}
						dataOutput.writeInt16(0);
					}
					break;
				}
				case RATIONAL:
				case URATIONAL:
				{
					Number v = (Number)entry.getValue();
					if (v.doubleValue() == v.intValue())
					{
						aWriter.writeInt32(1);
						aWriter.writeInt32(v.intValue());
					}
					else
					{
						aWriter.writeInt32(1000);
						aWriter.writeInt32((int)(v.doubleValue() * 1000));
					}
					break;
				}
				case ASCII:
				{
					byte [] chunk = entry.getValue().toString().getBytes();

					if (chunk.length < 4)
					{
						aWriter.writeInt32(chunk.length + 1);
						aWriter.write(chunk);
						aWriter.write(new byte[4 - chunk.length]);
					}
					else
					{
						aWriter.writeInt32(chunk.length + 1);
						aWriter.writeInt32(dataOffset + dataOutput.size());
						dataOutput.write(chunk);
						dataOutput.writeInt8(0);
					}
					break;
				}
				case UNDEFINED:
				{
					byte [] chunk;
					if (entry.getValue() instanceof byte[])
					{
						chunk = (byte[])entry.getValue();
					}
					else
					{
						chunk = entry.getValue().toString().getBytes();
					}
					if (chunk.length <= 4)
					{
						aWriter.writeInt32(chunk.length);
						aWriter.write(chunk);
						aWriter.write(new byte[4 - chunk.length]);
					}
					else
					{
						aWriter.writeInt32(chunk.length + 1);
						aWriter.writeInt32(dataOffset + dataOutput.size());
						dataOutput.write(chunk);
						dataOutput.writeInt8(0);
					}
					break;
				}
				default:
					throw new IllegalStateException("not implemented: " + entry);
			}

			assert ((aWriter.position() - 2 - mContentOffset) % 12) == 0;
		}

		int tablePointerOffset = aWriter.position();

		aWriter.write(new byte[12 * mTables.size() + 4]);

		mNextPointerOffset = aWriter.position() - 4;

		aWriter.write(dataOutput.toByteArray());

		mContentLength = aWriter.position() - mContentOffset;

		for (ExifTable table : mTables)
		{
			aWriter.position(aWriter.size());
			table.write(aWriter);
		}

		aWriter.position(tablePointerOffset);
		for (ExifTable table : mTables)
		{
			aWriter.writeInt16(ExifTag.ExifOffset.CODE);
			aWriter.writeInt16(ExifFormat.ULONG.ordinal());
			aWriter.writeInt32(1);
			aWriter.writeInt32(table.mContentOffset);
		}

		if (mThumbData != null)
		{
			thumbOffsetEntry.setValue(aWriter.size());

			aWriter.position(aWriter.size());
			aWriter.write(mThumbData);

			aWriter.position(thumbnailRecordOffset + 4);
			aWriter.writeInt32(1);
			aWriter.writeInt32((Integer)thumbOffsetEntry.getValue());
		}

		aWriter.position(mNextPointerOffset + 4);
	}


	void print(String aIndent)
	{
		System.out.println(aIndent + "Exif table");
		aIndent += "\t";

		for (ExifEntry entry : mEntries)
		{
			System.out.println(aIndent + entry);
		}

		for (ExifTable table : mTables)
		{
			table.print(aIndent);
		}
	}


	private static boolean validRange(int aOffset, int aLength, int aTag, Reader aReader)
	{
		if (aOffset >= 0 && aOffset + aLength <= aReader.capacity())
		{
			return true;
		}

		if (VERBOSE)
		{
			System.out.printf("  ERROR: Buffer overflow found: attempt to read at offset %d +%s, capacity %d, tag \"%s\" (0x%04x)%n", aOffset, aLength, aReader.capacity(), ExifTag.valueOf(aTag), aTag);
		}

		return false;
	}


	int getNextPointerOffset()
	{
		return mNextPointerOffset;
	}


	private byte[] toBuffer(int aValue, int aLength, boolean aBigEndian)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i = 0; i < aLength; i++)
		{
			int c = aBigEndian ? 0xff & (aValue >>> (8 * (3 - i))) : 0xff & (aValue >>> (8 * i));
			if (c == 0)
			{
				break;
			}
			baos.write((char)c);
		}
		return baos.toByteArray();
	}


	private String readString(Reader aReader, int aOffset, int aLength)
	{
		return new String(readBuffer(aReader, aOffset, aLength));
	}


	private byte[] readBuffer(Reader aReader, int aOffset, int aLength)
	{
		aReader.position(aOffset);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i = 0; i < aLength; i++)
		{
			int c = aReader.readInt8();
			if (c == 0)
			{
				aReader.grantAccess(aReader.position() - 1); // NOTE: some buggy encoders reuse ASCII zeros... (the trailing zero of one entry is the first zero of another)
				break;
			}
			baos.write(c);
		}
		return baos.toByteArray();
	}
}
