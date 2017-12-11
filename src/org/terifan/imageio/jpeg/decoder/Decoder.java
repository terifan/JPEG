package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;


public abstract class Decoder
{
	protected BitInputStream mBitStream;


	public Decoder(BitInputStream aBitStream)
	{
		mBitStream = aBitStream;
	}


	abstract void jinit_decoder(DecompressionState cinfo);

	abstract void finish_pass(DecompressionState cinfo);

	abstract void start_pass(DecompressionState cinfo);

	abstract boolean decode_mcu(DecompressionState cinfo, int[][] aCoefficients) throws IOException;
}
