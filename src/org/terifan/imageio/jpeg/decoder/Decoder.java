package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import org.terifan.imageio.jpeg.JPEG;


public abstract class Decoder
{
	abstract void initialize(JPEG aJPEG, JPEGBitInputStream aBitStream) throws IOException;

	abstract void finishPass(JPEG aJPEG) throws IOException;

	abstract void startPass(JPEG aJPEG) throws IOException;

	abstract boolean decodeMCU(JPEG aJPEG, int[][] aCoefficients) throws IOException;
}
