package org.terifan.imageio.jpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DHTSegment extends Segment
{
	private String mLog;


	public DHTSegment()
	{
	}


	@Override
	public DHTSegment decode(JPEG aJPEG, BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		mLog = "";

		while (length > 0)
		{
			HuffmanTable table = new HuffmanTable().decode(aBitStream);

			aJPEG.mHuffmanTables[table.getIndex()][table.getType()] = table;

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


	@Override
	public DHTSegment encode(JPEG aJPEG, BitOutputStream aBitStream) throws IOException
	{
		mLog = "";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		for (HuffmanTable table : aJPEG.mHuffmanDCTables)
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

		for (HuffmanTable table : aJPEG.mHuffmanACTables)
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
			for (HuffmanTable[] tables : aJPEG.mHuffmanTables)
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