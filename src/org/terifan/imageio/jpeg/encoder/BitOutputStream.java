package org.terifan.imageio.jpeg.encoder;

import java.io.IOException;
import java.io.OutputStream;


public class BitOutputStream extends OutputStream
{
	private OutputStream mOutputStream;
	private int mStreamOffset;


	public BitOutputStream(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
	}


	public void writeInt8(int aByte) throws IOException
	{
		mOutputStream.write(aByte);
		mStreamOffset++;
	}


	public void writeInt16(int aShort) throws IOException
	{
		mOutputStream.write(0xff & aShort >> 8);
		mOutputStream.write(0xff & aShort);
		mStreamOffset += 2;
	}


	@Override
	public void write(int aByte) throws IOException
	{
		mOutputStream.write(aByte);
		mStreamOffset++;
	}


	@Override
	public void write(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		mOutputStream.write(aBuffer, aOffset, aLength);
		mStreamOffset += aLength;
	}


	@Override
	public void close() throws IOException
	{
		if (mOutputStream != null)
		{
			mOutputStream.close();
			mOutputStream = null;
		}
	}


	public int getStreamOffset()
	{
		return mStreamOffset;
	}


	/**
	 * Writes a zero terminated string.
	 */
	public void writeString(String aString) throws IOException
	{
		write(aString.getBytes());
		writeInt8(0);
	}
}
