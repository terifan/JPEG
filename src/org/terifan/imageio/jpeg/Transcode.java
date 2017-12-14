package org.terifan.imageio.jpeg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriter;


public class Transcode
{
	public void transcode(InputStream aInputStream, OutputStream aOutputStream) throws IOException
	{
		JPEG jpeg = JPEGImageReader.decode(aInputStream);

		JPEGImageWriter writer = new JPEGImageWriter(aOutputStream);
		writer.create(jpeg);
		writer.encode(jpeg);
		writer.finish(jpeg);
	}
}
