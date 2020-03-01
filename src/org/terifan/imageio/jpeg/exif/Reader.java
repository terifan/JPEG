package org.terifan.imageio.jpeg.exif;

import java.io.IOException;
import java.nio.ByteBuffer;


class Reader
{
	private ByteBuffer mBuffer;
	private boolean mBigEndian;
	private boolean[] mAccessMap;


	public Reader(byte [] aData)
	{
		mBuffer = ByteBuffer.wrap(aData);
		mAccessMap = new boolean[aData.length];
		mBigEndian = true;
	}


	public int readInt8()
	{
		access(1);
		return mBuffer.get() & 0xff;
	}


	public int readInt16()
	{
		access(2);
		if (mBigEndian)
		{
			return mBuffer.getShort() & 0xffff;
		}
		return Short.reverseBytes(mBuffer.getShort()) & 0xffff;
	}


	public int readInt32()
	{
		access(4);
		if (mBigEndian)
		{
			return mBuffer.getInt();
		}
		return Integer.reverseBytes(mBuffer.getInt());
	}


	public void read(byte[] aBuffer)
	{
		access(aBuffer.length);
		mBuffer.get(aBuffer);
	}


	public int capacity()
	{
		return mBuffer.capacity();
	}


	public int position()
	{
		return mBuffer.position();
	}


	public void position(int aValue)
	{
		if (aValue > capacity())
		{
			throw new IllegalArgumentException("position " + aValue + " exceeds capacity " + capacity());
		}
		mBuffer.position(aValue);
	}


	public boolean isBigEndian()
	{
		return mBigEndian;
	}


	public void setBigEndian(boolean aState)
	{
		mBigEndian = aState;
	}


	public void grantAccess(int aOffset)
	{
		mAccessMap[aOffset] = false;
	}


	private void access(int aLength)
	{
//		System.out.println(mBuffer.position() + " .. " + (mBuffer.position() + aLength - 1));

//		if(mBuffer.position()==3378) throw new IllegalArgumentException();

		for (int i = mBuffer.position(); --aLength >= 0; i++)
		{
			if (mAccessMap[i])
			{
				throw new DataAccessException("Illegal access at offset " + i);
			}
			mAccessMap[i] = true;
		}
	}


	public void hexdump() throws IOException
	{
		int pos = mBuffer.position();

		String s = "";
		int i = 0;

		for (; mBuffer.position() < mBuffer.capacity(); i++)
		{
			int c = mBuffer.get() & 0xff;

			s += c < 32 ? '.' : (char)c;
			System.out.printf("%02x ", c);

			if ((i % 8) == 7)
			{
				System.out.print(" ");
			}
			if ((i % 40) == 39)
			{
				System.out.println(s);
				s = "";
			}
		}

		if ((i % 40) != 39)
		{
			System.out.println(new String(new byte[3 * (40 - (i % 40)) + (39 - (i % 40))/8]).replace('\u0000', ' ') + " " + s);
		}

		mBuffer.position(pos);
	}
}
