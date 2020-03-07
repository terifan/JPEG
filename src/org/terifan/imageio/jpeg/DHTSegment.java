package org.terifan.imageio.jpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DHTSegment extends Segment
{
	private JPEG mJPEG;


	public DHTSegment(JPEG aJPEG) throws IOException
	{
		mJPEG = aJPEG;
	}


	@Override
	public DHTSegment decode(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		while (length > 0)
		{
			HuffmanTable table = new HuffmanTable().decode(aBitStream);

			mJPEG.mHuffmanTables[table.getIndex()][table.getType()] = table;

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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		for (HuffmanTable table : mJPEG.dc_huff_tbl_ptrs)
		{
			if (table != null)
			{
				table.encode(baos);
			}
		}

		for (HuffmanTable table : mJPEG.ac_huff_tbl_ptrs)
		{
			if (table != null)
			{
				table.encode(baos);
			}
		}

		aBitStream.writeInt16(JPEGConstants.DHT);
		aBitStream.writeInt16(2 + baos.size());
		baos.writeTo(aBitStream);

		return this;
	}


	@Override
	public DHTSegment print(Log aLog) throws IOException
	{
		aLog.println("DHT segment");

		for (HuffmanTable table : mJPEG.dc_huff_tbl_ptrs)
		{
			if (table != null)
			{
				table.print(aLog);
			}
		}

		for (HuffmanTable table : mJPEG.ac_huff_tbl_ptrs)
		{
			if (table != null)
			{
				table.print(aLog);
			}
		}

		return this;
	}
}