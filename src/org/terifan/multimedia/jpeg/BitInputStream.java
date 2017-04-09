package org.terifan.multimedia.jpeg;

import java.io.IOException;
import java.io.InputStream;


class BitInputStream
{
	private InputStream mInputStream;
	private int mBitBuffer;
	private int mBitBufferLength;


	public BitInputStream(InputStream aInputStream)
	{
		mInputStream = aInputStream;
	}


	public int readInt8() throws IOException
	{
		if (mBitBufferLength != 0)
		{
			return readBits(8);
		}
		else
		{
			return mInputStream.read();
		}
	}


	public int readInt16() throws IOException
	{
		if (mBitBufferLength != 0)
		{
			return readBits(16);
		}
		else
		{
			int a = mInputStream.read();
			int b = mInputStream.read();
			if (b == -1)
			{
				return -1;
			}
			return (a << 8) + b;
		}
	}


	public void skipBits(int aLength) throws IOException
	{
		while (aLength > 24)
		{
			skipBits(24);
			aLength -= 24;
		}

		if (mBitBufferLength < aLength)
		{
			peekBits(aLength);
		}

		mBitBufferLength -= aLength;
		mBitBuffer &= (1 << mBitBufferLength) - 1;
	}


	public int readBits(int aLength) throws IOException
	{
		int value = peekBits(aLength);
		skipBits(aLength);
		return value;
	}


	public int peekBits(int aLength) throws IOException
	{
		assert aLength > 0 && aLength <= 24;

		while (mBitBufferLength < aLength)
		{
			int value = mInputStream.read();
			if (value == -1)
			{
				break;
			}

			if (value == 255)
			{
				value = mInputStream.read();
				if (value == -1)
				{
					break;
				}

				if (value == 0)
				{
					value = 255;
				}
				else
				{
					mBitBuffer <<= 8;
					mBitBuffer += 255;
					mBitBufferLength += 8;
				}
			}

			mBitBuffer <<= 8;
			mBitBuffer += value;
			mBitBufferLength += 8;
		}

		return (mBitBuffer >>> (mBitBufferLength - aLength));
	}


	public void skipBytes(int aByteCount) throws IOException
	{
		if (mBitBufferLength != 0)
		{
			skipBits(aByteCount << 3);
		}
		else
		{
			while (aByteCount > 0)
			{
				long s = mInputStream.skip(aByteCount);
				if (s <= 0)
				{
					IOException e = new IOException("Skip failed");
					e.printStackTrace(System.out);
					throw e;
				}
				aByteCount -= s;
			}
		}
	}


	/**
	 * Skipping remaining bits in the current byte so that the stream gets
	 * aligned to next byte.
	 */
	public void align() throws IOException
	{
		skipBits(mBitBufferLength & 7);
	}


	public void close() throws IOException
	{
		if (mInputStream != null)
		{
			mInputStream.close();
			mInputStream = null;
		}
	}
}