package org.terifan.imageio.jpeg.decoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.exif.JPEGExif;


public class JPEGImageParser
{
	private ArrayList<Chunk> mChunks;


	public JPEGImageParser(InputStream aInputStream) throws IOException
	{
		mChunks = new ArrayList<>();

		try (BitInputStream bitStream = new BitInputStream(aInputStream))
		{
			int nextSegment = bitStream.readInt16();

			if (nextSegment != SOI) // Start Of Image
			{
				throw new IOException("Error in JPEG stream; expected SOI segment marker but found: " + Integer.toString(nextSegment, 16));
			}

			for (;;)
			{
				nextSegment = bitStream.readInt16();

				if ((nextSegment & 0xFF00) != 0xFF00)
				{
					System.out.println("error");
					break;
				}

				if (VERBOSE)
				{
					System.out.println(Integer.toString(nextSegment, 16) + " -- " + bitStream.getStreamOffset() + " (" + Integer.toHexString(bitStream.getStreamOffset()) + ") -- " + getSOFDescription(nextSegment));
				}

				switch (nextSegment)
				{
					case EOI:
						return;
					case SOS:
					{
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						for (int c; (c = bitStream.read()) != -1;)
						{
							baos.write(c);
						}
						mChunks.add(new Chunk(nextSegment, baos.toByteArray(), true));
						return;
					}
					default:
					{
						int chunkLength = bitStream.readInt16();
						byte[] chunk = new byte[chunkLength - 2];
						bitStream.read(chunk);
						mChunks.add(new Chunk(nextSegment, chunk, false));
						break;
					}
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public JPEGExif getExif() throws IOException
	{
		for (Chunk chunk : mChunks)
		{
			if (chunk.mType == APP1)
			{
				JPEGExif exif = new JPEGExif();
				exif.read(new BitInputStream(new ByteArrayInputStream(chunk.mData)), chunk.mData.length);

				return exif;
			}
		}

		return null;
	}


	public void setExif(JPEGExif aExif) throws IOException
	{
		for (Chunk chunk : mChunks)
		{
			if (chunk.mType == APP1)
			{
				chunk.mData = aExif.encode();
			}
		}
	}


	public void write(OutputStream aOutputStream) throws IOException
	{
		aOutputStream.write(0xff & (SOI >> 8));
		aOutputStream.write(0xff & (SOI >> 0));

		for (Chunk chunk : mChunks)
		{
			aOutputStream.write(0xff & (chunk.mType >> 8));
			aOutputStream.write(0xff & (chunk.mType >> 0));

			if (!chunk.mFinalChunk)
			{
				aOutputStream.write(0xff & ((chunk.mData.length + 2) >> 8));
				aOutputStream.write(0xff & ((chunk.mData.length + 2) >> 0));
			}

			aOutputStream.write(chunk.mData);
		}
	}


	private static class Chunk
	{
		int mType;
		byte[] mData;
		boolean mFinalChunk;


		public Chunk(int aType, byte[] aData, boolean aFinalChunk)
		{
			mType = aType;
			mData = aData;
			mFinalChunk = aFinalChunk;
		}
	}
}
