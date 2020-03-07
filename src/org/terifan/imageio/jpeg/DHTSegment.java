package org.terifan.imageio.jpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public class DHTSegment implements Segment
{
	private JPEG mJPEG;


	public DHTSegment(JPEG aJPEG) throws IOException
	{
		mJPEG = aJPEG;
	}


	@Override
	public void read(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		if (length == 0)
		{
			return;
		}

		do
		{
			HuffmanTable table = new HuffmanTable().read(aBitStream);

			mJPEG.mHuffmanTables[table.getIndex()][table.getType()] = table;

			length -= 17 + table.getNumSymbols();

			if (length < 0)
			{
				throw new IOException("Error in JPEG stream; illegal DHT segment size.");
			}

			if (JPEGConstants.VERBOSE)
			{
				table.log();
			}
		}
		while (length > 0);
	}


	@Override
	public void write(BitOutputStream aBitStream) throws IOException
	{
		for (HuffmanTable table : mJPEG.dc_huff_tbl_ptrs)
		{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

			if (table != null && table.write(baos))
			{
				table.log();
			}

		if (baos.size() > 0)
		{
			aBitStream.writeInt16(JPEGConstants.DHT);
			aBitStream.writeInt16(2 + baos.size());
			baos.writeTo(aBitStream);
		}
		}

		for (HuffmanTable table : mJPEG.ac_huff_tbl_ptrs)
		{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

			if (table != null && table.write(baos))
			{
				table.log();
			}

		if (baos.size() > 0)
		{
			aBitStream.writeInt16(JPEGConstants.DHT);
			aBitStream.writeInt16(2 + baos.size());
			baos.writeTo(aBitStream);
		}
		}
	}
}