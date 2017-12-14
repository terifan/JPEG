package org.terifan.imageio.jpeg.exif;

import java.util.ArrayList;


public class Exif
{
	private final ArrayList<ExifEntry> mEntries;


	public Exif(ArrayList<ExifEntry> aEntries)
	{
		mEntries = aEntries;
	}


	public ArrayList<ExifEntry> list()
	{
		return mEntries;
	}


	public ExifEntry get(ExifTag aType)
	{
		for (ExifEntry entry : mEntries)
		{
			if (entry.getTag() == aType)
			{
				return entry;
			}
		}

		return null;
	}


	public <T> T value(ExifTag aType, Class<T> aCls)
	{
		for (ExifEntry entry : mEntries)
		{
			if (entry.getTag() == aType)
			{
				return (T)entry.getValue();
			}
		}

		return null;
	}
}
