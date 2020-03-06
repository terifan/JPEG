package org.terifan.imageio.jpeg.decoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.encoder.JPEGImageIO;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class JPEGImageReaderNGTest
{
	@Test
	public void testReadImage() throws IOException
	{
		try (InputStream input = JPEGImageReaderNGTest.class.getResourceAsStream("testimg.jpg"); InputStream input2 = JPEGImageReaderNGTest.class.getResourceAsStream("testimg.jpg"))
		{
			BufferedImage comparison = ImageIO.read(input2);

			BufferedImage image = new JPEGImageIO().read(input);

			assertEquals(image.getWidth(), 227);
			assertEquals(image.getHeight(), 149);

			assertEquals((int)PSNR.calculate(image, comparison), 39);
		}
	}
}
