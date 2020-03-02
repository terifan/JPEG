package org.terifan.imageio.jpeg.exif;

import java.io.IOException;
import java.util.ArrayList;


public class Exif
{
	protected ArrayList<ExifTable> mTables;
	protected boolean mBigEndian;


	public Exif()
	{
		mTables = new ArrayList<>();
		mBigEndian = true;
	}


	public Exif setBigEndian(boolean aBigEndian)
	{
		mBigEndian = aBigEndian;
		return this;
	}


	public boolean isBigEndian()
	{
		return mBigEndian;
	}


	public Exif addTable(ExifTable aTable)
	{
		mTables.add(aTable);
		return this;
	}


	public Exif removeTable(ExifTable aTable)
	{
		mTables.remove(aTable);
		return this;
	}


	public ExifTable getTable(int aTableIndex)
	{
		return mTables.get(aTableIndex);
	}


	public int getTableCount()
	{
		return mTables.size();
	}


	public Exif decode(byte[] aExifData) throws IOException
	{
		Reader reader = new Reader(aExifData);

		switch (reader.readInt16())
		{
			case 0x4d4d: // "MM"
			case 0x6d6d: // "mm"
				mBigEndian = true;
				break;
			case 0x4949: // "II"
			case 0x6969: // "ii"
				mBigEndian = false;
				break;
			default:
				throw new IOException("Bad TIFF encoding header");
		}

		reader.setBigEndian(mBigEndian);

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


	public byte[] encode() throws IOException
	{
		Writer writer = new Writer();
		writer.setBigEndian(mBigEndian);

		writer.write(new byte[8]); // placeholder for header

		ArrayList<Integer> pointerOffsets = new ArrayList<>();

		pointerOffsets.add(4);

		for (ExifTable table : mTables)
		{
			writer.position(writer.size());
			table.write(writer);
			pointerOffsets.add(table.getNextPointerOffset());
		}

		// write header
		writer.position(0);
		writer.writeInt16(writer.isBigEndian() ? 0x4d4d : 0x4949);
		writer.writeInt16(0x002a);

		for (int i = 0; i < mTables.size(); i++)
		{
			writer.position(pointerOffsets.get(i));
			writer.writeInt32(mTables.get(i).mContentOffset);
		}

		return writer.toByteArray();
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
}
