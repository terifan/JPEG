package org.terifan.imageio.jpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DHTSegment extends Segment
{
	private JPEG mJPEG;
	private String mLog;


	public DHTSegment(JPEG aJPEG) throws IOException
	{
		mJPEG = aJPEG;
	}


	@Override
	public DHTSegment decode(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		mLog = "";

		while (length > 0)
		{
			HuffmanTable table = new HuffmanTable().decode(aBitStream);

			mJPEG.mHuffmanTables[table.getIndex()][table.getType()] = table;

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
	public DHTSegment encode(BitOutputStream aBitStream) throws IOException
	{
		mLog = "";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		for (HuffmanTable table : mJPEG.dc_huff_tbl_ptrs)
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

		for (HuffmanTable table : mJPEG.ac_huff_tbl_ptrs)
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
	public DHTSegment print(Log aLog) throws IOException
	{
		aLog.println("DHT segment");

		if (mLog != null)
		{
			aLog.println(mLog);
		}

		if (aLog.isDetailed())
		{
			for (HuffmanTable[] tables : mJPEG.mHuffmanTables)
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