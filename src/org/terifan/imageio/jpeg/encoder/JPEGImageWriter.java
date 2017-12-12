package org.terifan.imageio.jpeg.encoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DQTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.SOSSegment;


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
		mJPEG.mArithmetic = true;

		mJPEG.arith_dc_L = new int[]{0,0};
		mJPEG.arith_dc_U = new int[]{1,1};
		mJPEG.arith_ac_K = new int[]{5,5};

		mBitStream = new BitOutputStream(aOutputStream);

		mBitStream.writeInt16(JPEGConstants.SOI);

		new APP0Segment(mJPEG).write(mBitStream);

		new DQTSegment(mJPEG).write(mBitStream);

		new SOFSegment(mJPEG, aImage.getWidth(), aImage.getHeight(), 8,
			new ComponentInfo(0, 0, 0, 2, 2),
			new ComponentInfo(1, 1, 1, 1, 1),
			new ComponentInfo(2, 2, 1, 1, 1)
		).write(mBitStream);

		new DACSegment(mJPEG).write(mBitStream);

		new SOSSegment(mJPEG).write(mBitStream);

		mBitStream.writeInt16(JPEGConstants.EOI);
	}
}
