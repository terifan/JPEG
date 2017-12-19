package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import java.io.InputStream;


public class BitInputStream
{
	private InputStream mInputStream;
	private int mBitBuffer;
	private int mBitBufferLength;
	private int mStreamOffset;
	private boolean mHandleEscapeChars;
	private boolean mHandleMarkers;
	private int mUnreadMarker;


	public BitInputStream(InputStream aInputStream)
	{
		mInputStream = aInputStream;
	}


	public int getUnreadMarker()
	{
		return mUnreadMarker;
	}


	public void setUnreadMarker(int aUnreadMarker)
	{
		mUnreadMarker = aUnreadMarker;
	}


	public void setHandleMarkers(boolean aHandleMarkers)
	{
		mHandleMarkers = aHandleMarkers;
	}


	public void setHandleEscapeChars(boolean aHandleEscapeChars)
	{
		mHandleEscapeChars = aHandleEscapeChars;
	}


	public int readInt8() throws IOException
	{
		if (mBitBufferLength != 0)
		{
			return readBits(8);
		}

		return readImpl();
	}


	public int readInt16() throws IOException
	{
		if (mBitBufferLength != 0)
		{
			return readBits(16);
		}

		int a = readImpl();
		int b = readImpl();
		if (b == -1)
		{
			return -1;
		}

		return (a << 8) + b;
	}


	public void skipBits(int aLength) throws IOException
	{
		skipBitsImpl(aLength);
	}


	public void skipBitsImpl(int aLength) throws IOException
	{
		while (aLength > 24)
		{
			skipBitsImpl(24);
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
		skipBitsImpl(aLength);

//		System.out.print("#"+aLength+"="+value+"# ");

		return value;
	}


	public int peekBits(int aLength) throws IOException
	{
//		System.out.print("/"+aLength+":"+mBitBufferLength+"/ ");

		assert aLength > 0 && aLength <= 24;

		while (mBitBufferLength < aLength)
		{
			int value = readImpl();
			if (value == -1)
			{
				break;
			}

			if (mHandleEscapeChars && value == 255)
			{
				value = readImpl();
				if (value == -1)
				{
					break;
				}

				if (value == 0)
				{
					value = 255;
				}
				else if (mHandleMarkers)
				{
					setUnreadMarker(value);
					continue;
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

		return mBitBuffer >>> (mBitBufferLength - aLength);
	}


	public void skipBytes(int aByteCount) throws IOException
	{
		if (mBitBufferLength != 0)
		{
			skipBitsImpl(aByteCount << 3);
		}
		else
		{
			while (aByteCount > 0)
			{
				long s = mInputStream.skip(aByteCount);
				mStreamOffset += aByteCount;
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
	 * Skipping remaining bits in the current byte so that the stream gets aligned to next byte.
	 */
	public void align() throws IOException
	{
		skipBitsImpl(mBitBufferLength & 7);
	}


	public void close() throws IOException
	{
		if (mInputStream != null)
		{
			mInputStream.close();
			mInputStream = null;
		}
	}


	private int readImpl() throws IOException
	{
		mStreamOffset++;
		int v = mInputStream.read();
		System.out.print("("+v+") ");
		return v;
	}


	public int getStreamOffset()
	{
		return mStreamOffset;
	}
}