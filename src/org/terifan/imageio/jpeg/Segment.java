package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public interface Segment
{
	void read(BitInputStream aBitStream) throws IOException;

	void write(BitOutputStream aBitStream) throws IOException;
}
