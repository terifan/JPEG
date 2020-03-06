package org.terifan.imageio.jpeg.exif;


public class ExifEntry
{
	private int mCode;
	private Object mValue;
	private ExifFormat mFormat;


	public ExifEntry(ExifTag aTag, Object aValue)
	{
		this(aTag.mFormat, aTag.CODE, aValue);
	}


	public ExifEntry(ExifFormat aFormat, int aTag, Object aValue)
	{
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
		return ExifTag.valueOf(mCode);
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


	@Override
	public String toString()
	{
		return String.format("%04X  %-10s %-25s %s %s", mCode, mFormat, ExifTag.valueOf(mCode), formatValue(), "");
	}


	private String formatValue()
	{
		if (mValue instanceof byte[])
		{
			byte[] bytes = (byte[])mValue;
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < bytes.length; i++)
			{
				sb.append(String.format("%02X", 0xff & bytes[i]));
			}

			return "0x" + sb.toString();
		}

		return mValue.toString();
	}
}
