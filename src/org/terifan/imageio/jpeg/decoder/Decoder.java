package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import org.terifan.imageio.jpeg.JPEG;


public abstract class Decoder
{
	abstract void initialize(BitInputStream aBitStream, JPEG cinfo) throws IOException;

	abstract void finishPass(JPEG cinfo) throws IOException;

	abstract void startPass(JPEG cinfo) throws IOException;

	abstract boolean decodeMCU(JPEG cinfo, int[][] aCoefficients) throws IOException;
}
