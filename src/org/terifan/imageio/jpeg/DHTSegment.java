package org.terifan.imageio.jpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static org.terifan.imageio.jpeg.JPEGConstants.NUM_HUFF_TBLS;
import org.terifan.imageio.jpeg.decoder.JPEGBitInputStream;
import org.terifan.imageio.jpeg.encoder.JPEGBitOutputStream;


public class DHTSegment extends Segment
{
	public HuffmanTable[][] mHuffmanTables;
	public HuffmanTable[] mHuffmanDCTables;
	public HuffmanTable[] mHuffmanACTables;

	private String mLog;


	public DHTSegment()
	{
		mHuffmanTables = new HuffmanTable[NUM_HUFF_TBLS][2];
		mHuffmanDCTables = new HuffmanTable[NUM_HUFF_TBLS];
		mHuffmanACTables = new HuffmanTable[NUM_HUFF_TBLS];
	}


	public DHTSegment decode(JPEGBitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		mLog = "";

		while (length > 0)
		{
			HuffmanTable table = new HuffmanTable().decode(aBitStream);

			mHuffmanTables[table.getIndex()][table.getType()] = table;

			if (mLog.length() > 0)
			{
				mLog += "\n";
			}
			mLog += "    index=" + table.getIndex() + ", type=" + (table.getType() == HuffmanTable.TYPE_DC ? "DC" : "AC");

			length -= 17 + table.getNumSymbols();

			if (length < 0)
			{
				throw new IOException("Error in JPEG stream; illegal DHT segment size.");
			}
		}

		return this;
	}


	public DHTSegment encode(JPEGBitOutputStream aBitStream) throws IOException
	{
		mLog = "";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		for (HuffmanTable table : mHuffmanDCTables)
		{
			if (table != null && !table.isSent())
			{
				if (mLog.length() > 0)
				{
					mLog += "\n";
				}
				mLog += "    index=" + table.getIndex() + ", type=" + (table.getType() == HuffmanTable.TYPE_DC ? "DC" : "AC");

				table.encode(baos);
			}
		}

		for (HuffmanTable table : mHuffmanACTables)
		{
			if (table != null && !table.isSent())
			{
				if (mLog.length() > 0)
				{
					mLog += "\n";
				}
				mLog += "    index=" + table.getIndex() + ", type=" + (table.getType() == HuffmanTable.TYPE_DC ? "DC" : "AC");

				table.encode(baos);
			}
		}

		if (baos.size() > 0)
		{
			aBitStream.writeInt16(SegmentMarker.DHT.CODE);
			aBitStream.writeInt16(2 + baos.size());
			baos.writeTo(aBitStream);
		}

		return this;
	}


	@Override
	public DHTSegment print(JPEG aJPEG, Log aLog) throws IOException
	{
		aLog.println("DHT segment");

		if (mLog != null)
		{
			aLog.println(mLog);
		}

		if (aLog.isDetailed())
		{
			for (HuffmanTable[] tables : mHuffmanTables)
			{
				for (HuffmanTable table : tables)
				{
					if (table != null)
					{
						table.print(aLog);
					}
				}
			}
		}

		return this;
	}
}