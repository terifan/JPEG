package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import java.io.InputStream;


public class BitInputStream extends InputStream
{
	private InputStream mInputStream;
	private int mBitBuffer;
	private int mBitBufferLength;
	private int mStreamOffset;
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


	@Override
	public int read() throws IOException
	{
		return readInt8();
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


	public int readInt32() throws IOException
	{
		return (readInt16() << 16) + readInt16();
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

		while (mBitBufferLength < aLength)
		{
			int data;

//			if (mHandleMarkers)
//			{
//				if (getUnreadMarker() != 0)
//				{
//					data = 0;
//				}
//				else
//				{
//					data = readImpl();
//					/* read next input byte */
//					if (data == 0xFF)
//					{
//						/* zero stuff or marker code */
//						do
//						{
//							data = readImpl();
//						}
//						while (data == 0xFF);
//						/* swallow extra 0xFF bytes */
//						if (data == 0)
//						{
//							data = 0xFF;	/* discard stuffed zero byte */
//						}
//						else
//						{
//							/* Note: Different from the Huffman decoder, hitting
//							* a marker while processing the compressed data
//							* segment is legal in arithmetic coding.
//							* The convention is to supply zero data
//							* then until decoding is complete.
//							 */
//							setUnreadMarker(data);
//							data = 0;
//						}
//					}
//				}
//			}
//			else
			{
				data = readImpl();
			}

			if (data == 0xff)
			{
				do
				{
					data = readImpl();
				}
				while (data == 0xff);

				if (data == 0)
				{
					data = 0xff;
				}
//				else if (mHandleMarkers)
//				{
//					setUnreadMarker(data);
//
//					mBitBuffer <<= 8;
//					mBitBuffer += 0;
//					mBitBufferLength += 8;
//
//					throw new IOException();
//				}
				else
				{
					mBitBuffer <<= 8;
					mBitBuffer += 0xff;
					mBitBufferLength += 8;
				}
			}

			mBitBuffer <<= 8;
			mBitBuffer += data;
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


	@Override
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
//		System.out.print("("+v+") ");
		return v;
	}


	public int getStreamOffset()
	{
		return mStreamOffset;
	}


	public byte[] readBytes(byte[] aBuffer) throws IOException
	{
		for (int i = 0; i < aBuffer.length; i++)
		{
			aBuffer[i] = (byte)readImpl();
		}

		return aBuffer;
	}


	public String readString() throws IOException
	{
		StringBuilder sb = new StringBuilder();

		for (int c; (c = readInt8()) != 0;)
		{
			sb.append((char)c);
		}

		return sb.toString();
	}
}