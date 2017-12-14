package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import org.terifan.imageio.jpeg.JPEG;


public abstract class Decoder
{
	protected BitInputStream mBitStream;


	public Decoder(BitInputStream aBitStream)
	{
		mBitStream = aBitStream;
	}


	abstract void initialize(JPEG cinfo);

	abstract void finishPass(JPEG cinfo);

	abstract void startPass(JPEG cinfo);

	abstract boolean decodeMCU(JPEG cinfo, int[][] aCoefficients) throws IOException;
}
