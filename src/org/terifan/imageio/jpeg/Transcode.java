package org.terifan.imageio.jpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.JPEGImageWriter;


public class Transcode 
{
	public void transcode(InputStream aInputStream) throws IOException
	{
		JPEGImageReader reader = new JPEGImageReader().load(aInputStream);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		JPEGImageWriter writer = new JPEGImageWriter(out);

		writer.encode(reader.getJPEG(), reader.getDctCoefficients());
	}
}
