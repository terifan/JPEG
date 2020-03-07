package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;


public class Debug
{
	public static void hexdump(BitInputStream aInput) throws IOException
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
}
