package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.PrintStream;
import org.terifan.imageio.jpeg.decoder.BitInputStream;
import org.terifan.imageio.jpeg.encoder.BitOutputStream;


public abstract class Segment<T extends Segment>
{
	abstract T decode(BitInputStream aBitStream) throws IOException;

	abstract T encode(BitOutputStream aBitStream) throws IOException;

	abstract T print(Log aLog) throws IOException;

	public T log(Log aLog) throws IOException
	{
		if (aLog != null)
		{
			print(aLog);
		}
		return (T)this;
	}
}
