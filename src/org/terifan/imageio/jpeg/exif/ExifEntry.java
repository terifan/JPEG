package org.terifan.imageio.jpeg.exif;


public class ExifEntry
{
	private ExifTag mTag;
	private Object mValue;


	public ExifEntry(ExifTag aTag, Object aValue)
	{
		mTag = aTag;
		mValue = aValue;
	}


	public ExifTag getTag()
	{
		return mTag;
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


	@Override
	public String toString()
	{
//		return String.format("%04X  %-10s %-25s %s %s", mCode, mFormat, ExifTag.valueOf(mCode), formatValue(), "");
		return String.format(mTag.name + ": " + formatValue());
	}


	private String formatValue()
	{
		if (mValue instanceof byte[] v)
		{
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < v.length; i++)
			{
				sb.append(String.format("%02X", 0xff & v[i]));
			}

			return "0x" + sb.toString();
		}

		return mValue.toString();
	}
}
