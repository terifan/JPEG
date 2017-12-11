package org.terifan.imageio.jpeg.encoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.JPEGConstants;


public class JPEGImageWriter 
{
	private BitOutputStream mBitStream;
	
	
	public void write(BufferedImage aImage, OutputStream aOutputStream) throws IOException
	{
		mBitStream = new BitOutputStream(aOutputStream);

		mBitStream.writeInt16(JPEGConstants.SOI);
		
		writeAPP0Segment();
	}


	private void writeAPP0Segment() throws IOException
	{
		mBitStream.writeInt16(JPEGConstants.APP0);
		mBitStream.writeInt16(16);
		mBitStream.write("JFIF".getBytes(), 0, 4);
	}
}
