package org.terifan.imageio.jpeg.exif;

import java.io.IOException;
import java.util.Arrays;


class Writer
{
	private byte[] mBuffer;
	private int mSize;
	private int mPosition;
	private boolean mBigEndian;


	public Writer()
	{
		mBuffer = new byte[1024];
	}


	public Writer(byte[] aBuffer)
	{
		mBuffer = aBuffer;
	}


	public byte[] toByteArray()
	{
		return Arrays.copyOfRange(mBuffer, 0, mSize);
	}


	public Writer writeInt8(int aValue) throws IOException
	{
		if (mPosition == mBuffer.length)
		{
			mBuffer = Arrays.copyOfRange(mBuffer, 0, mBuffer.length * 3 / 2);
		}

		mBuffer[mPosition++] = (byte)aValue;
		if (mPosition > mSize)
		{
			mSize = mPosition;
		}

		return this;
	}


	public Writer writeInt16(int aValue) throws IOException
	{
		if (mBigEndian)
		{
			writeInt8(0xff & (aValue >> 8));
			writeInt8(0xff & aValue);
		}
		else
		{
			writeInt8(0xff & aValue);
			writeInt8(0xff & (aValue >> 8));
		}

		return this;
	}


	public Writer writeInt32(int aValue) throws IOException
	{
		if (mBigEndian)
		{
			writeInt16(0xffff & (aValue >>> 16));
			writeInt16(0xffff & aValue);
		}
		else
		{
			writeInt16(0xffff & aValue);
			writeInt16(0xffff & (aValue >>> 16));
		}

		return this;
	}


	public Writer write(byte[] aBytes) throws IOException
	{
		if (mPosition + aBytes.length > mBuffer.length)
		{
			mBuffer = Arrays.copyOfRange(mBuffer, 0, (mBuffer.length + aBytes.length) * 3 / 2);
		}

		System.arraycopy(aBytes, 0, mBuffer, mPosition, aBytes.length);
		mPosition += aBytes.length;
		if (mPosition > mSize)
		{
			mSize = mPosition;
		}

		return this;
	}


	public int position()
	{
		return mPosition;
	}


	public Writer position(int aPosition)
	{
		mPosition = aPosition;
		return this;
	}


	public int size()
	{
		return mSize;
	}


	public boolean isBigEndian()
	{
		return mBigEndian;
	}


	public Writer setBigEndian(boolean aBigEndian)
	{
		mBigEndian = aBigEndian;
		return this;
	}
}
