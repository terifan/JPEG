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


	abstract void jinit_decoder(JPEG cinfo);

	abstract void finish_pass(JPEG cinfo);

	abstract void start_pass(JPEG cinfo);

	abstract boolean decode_mcu(JPEG cinfo, int[][] aCoefficients) throws IOException;
}
