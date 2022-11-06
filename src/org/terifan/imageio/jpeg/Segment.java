package org.terifan.imageio.jpeg;

import java.io.IOException;


public abstract class Segment<T extends Segment>
{
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
