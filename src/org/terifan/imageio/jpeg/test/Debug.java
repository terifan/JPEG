package org.terifan.imageio.jpeg.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;


public class Debug 
{
	public static void hexDump(Buffer aBuffer)
	{
		hexDump((byte[])aBuffer.array(), 0, aBuffer.limit());
	}


	public static void hexDump(byte [] aBuffer)
	{
		hexDump(aBuffer, 0, aBuffer.length);
	}


	public static void hexDump(byte [] aBuffer, int aLength)
	{
		hexDump(aBuffer, 0, aLength);
	}


	public static void hexDump(byte [] aBuffer, int aOffset, int aLength)
	{
		hexDump(new ByteArrayInputStream(aBuffer, aOffset, aLength), aLength);
	}


	public static void hexDump(InputStream aInputStream, int aLength)
	{
		int LW = 64;

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
