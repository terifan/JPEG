package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.PrintStream;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;
import static org.terifan.imageio.jpeg.QuantizationTable.PRECISION_16_BITS;
import static org.terifan.imageio.jpeg.QuantizationTable.PRECISION_8_BITS;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DQTSegment extends Segment
{
	private JPEG mJPEG;


	public DQTSegment(JPEG aJPEG)
	{
		mJPEG = aJPEG;
	}


	@Override
	public DQTSegment decode(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		do
		{
			QuantizationTable table = readTable(aBitStream);

			mJPEG.mQuantizationTables[table.getIdentity()] = table;

			length -= 1 + (table.getPrecision() == PRECISION_8_BITS ? 64 : 128);

			if (length < 0)
			{
				throw new IOException("Error in JPEG stream; illegal DQT segment size.");
			}
		}
		while (length > 0);

		return this;
	}


	@Override
	public DQTSegment encode(BitOutputStream aBitStream) throws IOException
	{
		for (QuantizationTable table : mJPEG.mQuantizationTables)
		{
			if (table == null)
			{
				continue;
			}

			int len = 2 + 1 + (table.getPrecision() == PRECISION_8_BITS ? 64 : 128);

			aBitStream.writeInt16(SegmentMarker.DQT.CODE);
			aBitStream.writeInt16(len);
			aBitStream.writeInt8(((table.getPrecision() == PRECISION_8_BITS ? 0 : 1) << 3) | table.getIdentity());

			double[] data = table.getDivisors();

			for (int i = 0; i < 64; i++)
			{
				double v = data[NATURAL_ORDER[i]];

				if (table.getPrecision() == PRECISION_16_BITS)
				{
					v *= 256.0;
				}

				aBitStream.write((int)v);
			}
		}

		return this;
	}


	private QuantizationTable readTable(BitInputStream aBitStream) throws IOException
	{
		int temp = aBitStream.readInt8();
		int identity = temp & 0x07;
		int precision = (temp >> 3) == 0 ? PRECISION_8_BITS : PRECISION_16_BITS;

		double[] data = new double[64];

		for (int i = 0; i < 64; i++)
		{
			if (precision == PRECISION_8_BITS)
			{
				data[NATURAL_ORDER[i]] = aBitStream.readInt8();
			}
			else
			{
				data[NATURAL_ORDER[i]] = aBitStream.readInt16() / 256.0;
			}
		}

		return new QuantizationTable(identity, precision, data);
	}


	@Override
	public DQTSegment print(Log aLog) throws IOException
	{
		aLog.println("DQT segment");
		for (QuantizationTable table : mJPEG.mQuantizationTables)
		{
			if (table != null)
			{
				aLog.println("  identity=" + table.getIdentity() + ", precision=" + (table.getPrecision() == PRECISION_8_BITS ? 8 : 16) + " bits");

				if (aLog.isDetailed())
				{
					table.print(aLog);
				}
			}
		}
		return this;
	}
}
