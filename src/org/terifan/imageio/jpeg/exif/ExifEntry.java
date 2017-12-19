package org.terifan.imageio.jpeg.exif;


public class ExifEntry
{
	private int mCode;
	private Object mValue;
	private ExifFormat mFormat;
	private byte [] mExifData;


	public ExifEntry(ExifTag aFieldType, Object aValue)
	{
		this(null, aFieldType.mFormat, aFieldType.mCode, aValue);
	}


	public ExifEntry(byte [] aExifData, ExifFormat aFormat, int aTag, Object aValue)
	{
		mExifData = aExifData;
		mFormat = aFormat;
		mCode = aTag;
		mValue = aValue;
	}


	public int getCode()
	{
		return mCode;
	}


	public ExifTag getTag()
	{
		return ExifTag.lookup(mCode);
	}


	public Object getValue()
	{
		return mValue;
	}


	public ExifFormat getFormat()
	{
		return mFormat;
	}


	public byte [] getExifData()
	{
		return mExifData;
	}


	@Override
	public String toString()
	{
		return String.format("tag=%X format=%s type=%s value=%s", mCode, mFormat, ExifTag.decode(mCode), mValue);
	}
}
