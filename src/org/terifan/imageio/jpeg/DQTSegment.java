package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.Serializable;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import static org.terifan.imageio.jpeg.JPEGConstants.NATURAL_ORDER;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DQTSegment extends Segment implements Serializable
{
	private final static long serialVersionUID = 1L;

	public final static int PRECISION_8_BITS = 1;
	public final static int PRECISION_16_BITS = 2;

	private QuantizationTable[] mTables;


	public DQTSegment()
	{
		mTables = new QuantizationTable[8];
	}


	public QuantizationTable getTable(int aIndex)
	{
		QuantizationTable table = mTables[aIndex];
		if (table == null)
		{
			throw new IllegalArgumentException("No such table: " + aIndex);
		}
		return table;
	}


	public DQTSegment setTable(int aIndex, QuantizationTable aTable)
	{
		mTables[aIndex] = aTable;
		return this;
	}


	public static class QuantizationTable implements Serializable
	{
		private final static long serialVersionUID = 1L;

		/**
		 * 16-bit quantization values.
		 */
		private int[] mDivisors;
		private int mPrecision;
		private int mIdentity;


		/**
		 * @param aDivisors 8 or 16 bit values depending on the precision parameter, 8 bit values will be scaled to 16 bits.
		 */
		public QuantizationTable(int aIdentity, int aPrecision, int... aDivisors)
		{
			mIdentity = aIdentity;
			mPrecision = aPrecision;
			mDivisors = new int[64];

			boolean b = aPrecision == PRECISION_8_BITS;

			for (int i = 0; i < 64; i++)
			{
				mDivisors[i] = b ? aDivisors[i] << 8 : aDivisors[i];
			}
		}


		public int[] getDivisors()
		{
			return mDivisors;
		}


		public int getIdentity()
		{
			return mIdentity;
		}


		public int getPrecision()
		{
			return mPrecision;
		}


		public void print(Log aLog)
		{
			for (int row = 0, i = 0; row < 8; row++)
			{
				aLog.print("  ");
				for (int col = 0; col < 8; col++, i++)
				{
					aLog.print("%7.3f ", mDivisors[i]);
				}
				aLog.println("");
			}
		}
	}


	@Override
	public DQTSegment decode(JPEG aJPEG, BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		do
		{
			int temp = aBitStream.readInt8();
			int identity = temp & 0x07;
			int precision = (temp >> 3) == 0 ? PRECISION_8_BITS : PRECISION_16_BITS;

			int[] data = new int[64];

			for (int i = 0; i < 64; i++)
			{
				if (precision == PRECISION_8_BITS)
				{
					data[NATURAL_ORDER[i]] = aBitStream.readInt8();
				}
				else
				{
					data[NATURAL_ORDER[i]] = aBitStream.readInt16();
				}
			}

			QuantizationTable table = mTables[identity] = new QuantizationTable(identity, precision, data);

			length -= 1 + (table.getPrecision() == PRECISION_8_BITS ? 1 * 64 : 2 * 64);

			if (length < 0)
			{
				throw new IOException("Error in JPEG stream; illegal DQT segment size.");
			}
		}
		while (length > 0);

		return this;
	}


	@Override
	public DQTSegment encode(JPEG aJPEG, BitOutputStream aBitStream) throws IOException
	{
		for (QuantizationTable table : mTables)
		{
			if (table == null)
			{
				continue;
			}

			int len = 2 + 1 + (table.getPrecision() == PRECISION_8_BITS ? 1 * 64 : 2 * 64);

			aBitStream.writeInt16(SegmentMarker.DQT.CODE);
			aBitStream.writeInt16(len);
			aBitStream.writeInt8(((table.getPrecision() == PRECISION_8_BITS ? 0 : 1) << 3) | table.getIdentity());

			int[] data = table.getDivisors();

			for (int i = 0; i < 64; i++)
			{
				int v = data[NATURAL_ORDER[i]];

				if (table.getPrecision() == PRECISION_8_BITS)
				{
					aBitStream.writeInt8(v >> 8);
				}
				else
				{
					aBitStream.writeInt16(v);
				}
			}
		}

		return this;
	}


	@Override
	public DQTSegment print(JPEG aJPEG, Log aLog) throws IOException
	{
		aLog.println("DQT segment");
		for (QuantizationTable table : mTables)
		{
			if (table != null)
			{
				aLog.println("  identity=%d, precision=%d bits", table.getIdentity(), table.getPrecision() == PRECISION_8_BITS ? 8 : 16);

				if (aLog.isDetailed())
				{
					table.print(aLog);
				}
			}
		}
		return this;
	}
}
