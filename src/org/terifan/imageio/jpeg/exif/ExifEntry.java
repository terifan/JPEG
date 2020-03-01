package org.terifan.imageio.jpeg.exif;


public class ExifEntry
{
	private int mCode;
	private Object mValue;
	private ExifFormat mFormat;
	private byte [] mExifData;
	private String mDebug;


	public ExifEntry(ExifTag aFieldType, Object aValue)
	{
		this(null, aFieldType.mFormat, aFieldType.CODE, aValue);
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


	public ExifEntry setValue(Object aValue)
	{
		mValue = aValue;
		return this;
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
		return String.format("%04X  %-10s %-25s %s %s", mCode, mFormat, ExifTag.decode(mCode), mValue instanceof byte[] ? toHexString((byte[])mValue) : mValue, "");
	}


	private String toHexString(byte[] aValue)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < aValue.length; i++)
		{
			sb.append(String.format("%02X", 0xff & aValue[i]));
		}
		return "0x" + sb.toString();
	}


	ExifEntry setDebug(String aDebug)
	{
		mDebug = aDebug;
		return this;
	}


	String getDebug()
	{
		return mDebug;
	}
}
