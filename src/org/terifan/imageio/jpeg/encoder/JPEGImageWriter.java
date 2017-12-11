package org.terifan.imageio.jpeg.encoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.DQTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.QuantizationTable;


public class JPEGImageWriter 
{
	private BitOutputStream mBitStream;
	private JPEG mJPEG;
	
	
	public void write(BufferedImage aImage, int aQuality, OutputStream aOutputStream) throws IOException
	{
		mJPEG = new JPEG();
		
		mJPEG.mQuantizationTables = new QuantizationTable[2];
		mJPEG.mQuantizationTables[0] = QuantizationTableFactory.buildQuantTable(aQuality, 0);
		mJPEG.mQuantizationTables[1] = QuantizationTableFactory.buildQuantTable(aQuality, 1);

		mBitStream = new BitOutputStream(aOutputStream);

		mBitStream.writeInt16(JPEGConstants.SOI);
		
		new APP0Segment(mJPEG).write(mBitStream);

		new DQTSegment(mJPEG).write(mBitStream);


		mBitStream.writeInt16(JPEGConstants.EOI);
	}
}
