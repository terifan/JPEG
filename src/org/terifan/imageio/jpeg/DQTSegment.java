package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;
import static org.terifan.imageio.jpeg.QuantizationTable.PRECISION_16_BITS;
import static org.terifan.imageio.jpeg.QuantizationTable.PRECISION_8_BITS;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DQTSegment
{
	private JPEG mJPEG;


	public DQTSegment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	public void read(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;
		
		do
		{
			QuantizationTable table = readTable(aBitStream);

			mJPEG.mQuantizationTables[table.getIdentity()] = table;

			length -= 1 + table.getPrecision() * 64;

			if (length < 0)
			{
				throw new IOException("Error in JPEG stream; illegal DQT segment size.");
			}
		}
		while (length > 0);
	}
	
	
	private QuantizationTable readTable(BitInputStream aBitStream) throws IOException
	{
		int temp = aBitStream.readInt8();
		int identity = temp & 0x07;
		int precision = (temp >> 3) == 0 ? PRECISION_8_BITS : PRECISION_16_BITS;

		double[] table = new double[64];

		for (int i = 0; i < 64; i++)
		{
			if (precision == PRECISION_8_BITS)
			{
				table[NATURAL_ORDER[i]] = aBitStream.readInt8();
			}
			else
			{
				table[NATURAL_ORDER[i]] = aBitStream.readInt16() / 256.0;
			}
		}

		if (VERBOSE)
		{
			System.out.println("DQTMarkerSegment[identity=" + identity + ", precision=" + (precision == PRECISION_8_BITS ? 8 : 16) + "]");

			for (int row = 0, i = 0; row < 8; row++)
			{
				System.out.print(" ");
				for (int col = 0; col < 8; col++, i++)
				{
					System.out.printf("%7.3f ", table[i]);
				}
				System.out.println();
			}
		}

		return new QuantizationTable(identity, precision, table);
	}

	
	public void write(BitOutputStream aBitStream) throws IOException
	{
		for (QuantizationTable table : mJPEG.mQuantizationTables)
		{
			aBitStream.writeInt16(JPEGConstants.DQT);
			aBitStream.writeInt16(2 + 1 + 64 * table.getPrecision());

			aBitStream.writeInt8((table.getPrecision() == PRECISION_16_BITS ? 1 : 0) | table.getIdentity()); // 8 bit precision

			for (int i = 0; i < 64; i++)
			{
				double v = table.getDivisors()[NATURAL_ORDER[i]];
				
				if (table.getPrecision() == PRECISION_16_BITS)
				{
					v *= 256;
				}

				aBitStream.write((int)v);
			}
		}
	}
}
