package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriter;


public class Transcode
{
	public void transcode(URL aInputStream, OutputStream aOutputStream) throws IOException
	{
		try (InputStream in = aInputStream.openStream())
		{
			transcode(in, aOutputStream);
		}
	}


	public void transcode(InputStream aInputStream, OutputStream aOutputStream) throws IOException
	{
		JPEG jpeg = JPEGImageReader.decode(aInputStream);

		if (jpeg.components == null)
		{
			throw new IllegalStateException("Error decoding source image");
		}

		JPEGImageWriter writer = new JPEGImageWriter(aOutputStream);
		writer.create(jpeg);
		writer.encode(jpeg);
		writer.finish(jpeg);
	}
}
