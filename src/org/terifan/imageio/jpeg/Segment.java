package org.terifan.imageio.jpeg;

import java.io.IOException;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public abstract class Segment<T extends Segment>
{
	abstract T decode(JPEG aJPEG, BitInputStream aBitStream) throws IOException;

	abstract T encode(JPEG aJPEG, BitOutputStream aBitStream) throws IOException;

	abstract T print(JPEG aJPEG, Log aLog) throws IOException;

	public T log(JPEG aJPEG, Log aLog) throws IOException
	{
		if (aLog != null)
		{
			print(aJPEG, aLog);
		}
		return (T)this;
	}
}
