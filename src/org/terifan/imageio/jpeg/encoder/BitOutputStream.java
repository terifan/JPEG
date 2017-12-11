package org.terifan.imageio.jpeg.encoder;

import java.io.IOException;
import java.io.OutputStream;


public class BitOutputStream extends OutputStream
{
	private OutputStream mOutputStream;
	private int mBitsToGo;
	private int mBitBuffer;


	public BitOutputStream(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
		mBitBuffer = 0;
		mBitsToGo = 8;
	}


	public void writeBit(int aBit) throws IOException
	{
		mBitBuffer |= aBit << --mBitsToGo;

		if (mBitsToGo == 0)
		{
			mOutputStream.write(mBitBuffer & 0xFF);
			mBitBuffer = 0;
			mBitsToGo = 8;
		}
	}


	public void writeBits(int aValue, int aLength) throws IOException
	{
		while (aLength-- > 0)
		{
			writeBit((aValue >>> aLength) & 1);
		}
	}


	public void writeBits(long aValue, int aLength) throws IOException
	{
		if (aLength > 32)
		{
			writeBits((int)(aValue >>> 32), aLength - 32);
		}

		writeBits((int)(aValue), Math.min(aLength, 32));
	}


	public void writeInt8(int aByte) throws IOException
	{
		writeBits(0xff & aByte, 8);
	}


	public void writeInt16(int aShort) throws IOException
	{
		writeBits(0xffff & aShort, 16);
	}


	@Override
	public void write(int aByte) throws IOException
	{
		writeBits(0xff & aByte, 8);
	}


	@Override
	public void write(byte[] aBuffer) throws IOException
	{
		write(aBuffer, 0, aBuffer.length);
	}


	@Override
	public void write(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		if (mBitsToGo == 8)
		{
			mOutputStream.write(aBuffer, aOffset, aLength);
		}
		else
		{
			while (aLength-- > 0)
			{
				writeBits(aBuffer[aOffset++] & 0xFF, 8);
			}
		}
	}


	@Override
	public void close() throws IOException
	{
		if (mOutputStream != null)
		{
			align();

			mOutputStream.close();
			mOutputStream = null;
		}
	}


	public void align() throws IOException
	{
		if (mBitsToGo < 8)
		{
			writeBits(0, mBitsToGo);
		}
	}


	public int getBitCount()
	{
		return 8-mBitsToGo;
	}
}
