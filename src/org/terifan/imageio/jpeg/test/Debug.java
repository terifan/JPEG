package org.terifan.imageio.jpeg.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class Debug
{
	public static void hexDump(Buffer aBuffer)
	{
		Debug.hexDump((byte[])aBuffer.array(), 0, aBuffer.limit());
	}


	public static void hexDump(byte [] aBuffer)
	{
		Debug.hexDump(aBuffer, 0, aBuffer.length);
	}


	public static void hexDump(byte [] aBuffer, int aLength)
	{
		Debug.hexDump(aBuffer, 0, aLength);
	}


	public static void hexDump(byte [] aBuffer, int aOffset, int aLength)
	{
		Debug.hexDump(new ByteArrayInputStream(aBuffer, aOffset, aLength), aLength);
	}


	public static void hexDump(BitInputStream aInput) throws IOException
	{
		int streamOffset = aInput.getStreamOffset();

		int cnt = 0;
		int b1 = 0;
		for (int r = 0; r < 1000; r++)
		{
			for (int c = 0; c < 96; c++, cnt++)
			{
				int b0 = aInput.readInt8();

				if (b0 == -1)
				{
					return;
				}

				System.out.printf("%02x ", b0);

				if (b1 == 255 && b0 != 0)
				{
					System.out.println();
					System.out.println("=> " + streamOffset + " +" + cnt + " (" + Integer.toHexString(streamOffset) + ")");
					return;
				}

				b1 = b0;
				if ((c % 8) == 7)
				{
					System.out.print(" ");
				}
			}
			System.out.println();
		}
	}


	public static void hexDump(InputStream aInputStream, int aLength)
	{
		int LW = 40;

		try
		{
			StringBuilder binText = new StringBuilder("");
			StringBuilder hexText = new StringBuilder("");

			for (int row = 0; row == 0 || aLength > 0; row++)
			{
				hexText.append(String.format("%04d: ", row * LW));

				int padding = 3 * LW + LW / 8;

				for (int i = 0; i < LW && aLength > 0; i++)
				{
					int c = aInputStream.read();

					if (c == -1)
					{
						aLength = 0;
						break;
					}

					hexText.append(String.format("%02x ", c));
					binText.append(Character.isISOControl(c) ? '.' : (char)c);
					padding -= 3;
					aLength--;

					if ((i & 7) == 7)
					{
						hexText.append(" ");
						padding--;
					}
				}

				for (int i = 0; i < padding; i++)
				{
					hexText.append(" ");
				}

				System.out.println(hexText.append(binText).toString());

				binText.setLength(0);
				hexText.setLength(0);
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}


	public static void printTables(int[][] aInput)
	{
		for (int r = 0; r < 8; r++)
		{
			for (int t = 0; t < aInput.length; t++)
			{
				for (int c = 0; c < 8; c++)
				{
					System.out.printf("%5d ", aInput[t][r*8+c]);
				}
				System.out.print(r == 4 && t < aInput.length-1 ? "  ===>" : "      ");
			}
			System.out.println();
		}
	}
}
