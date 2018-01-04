package org.terifan.imageio.jpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static org.terifan.imageio.jpeg.HuffmanTable.TYPE_AC;
import static org.terifan.imageio.jpeg.JPEGConstants.VERBOSE;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;
import org.terifan.imageio.jpeg.encoder.HuffmanEncoder;


public class DHTSegment
{
	private JPEG mJPEG;


	public DHTSegment(JPEG aJPEG) throws IOException
	{
		mJPEG = aJPEG;
	}


	public void read(BitInputStream aBitStream) throws IOException
	{
		int length = aBitStream.readInt16() - 2;

		do
		{
			HuffmanTable table = new HuffmanTable().read(aBitStream);

			mJPEG.mHuffmanTables[table.getIndex()][table.getType()] = table;

			length -= 17 + table.getNumSymbols();

			if (length < 0)
			{
				throw new IOException("Error in JPEG stream; illegal DHT segment size.");
			}
		}
		while (length > 0);
	}


	public void write(BitOutputStream aBitStream) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		for (HuffmanTable table : mJPEG.dc_huff_tbl_ptrs)
		{
			if (table != null)
			{
				table.log();
				
				table.write(baos);
			}
		}
		for (HuffmanTable table : mJPEG.ac_huff_tbl_ptrs)
		{
			if (table != null)
			{
				table.log();
			
				table.write(baos);
			}
		}
		
//		for (HuffmanTable[] tables : mJPEG.mHuffmanTables)
//		{
//			for (HuffmanTable table : tables)
//			{
//				table.write(baos);
//			}
//		}

		aBitStream.writeInt16(JPEGConstants.DHT);
		aBitStream.writeInt16(2 + baos.size());
//		baos.writeTo(aBitStream);

		aBitStream.write(baos.toByteArray());
	}
}