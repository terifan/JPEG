package org.terifan.imageio.jpeg.decoder;

import examples._ImageQualityTest;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.JPEGImageIO;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class JPEGImageReaderNGTest
{
	@Test
	public void testReadImage() throws IOException
	{
		try (InputStream input1 = JPEGImageReaderNGTest.class.getResourceAsStream("testimg.jpg"); InputStream input2 = JPEGImageReaderNGTest.class.getResourceAsStream("testimg.jpg"))
		{
			BufferedImage image = new JPEGImageIO().read(input1);
			BufferedImage comparison = ImageIO.read(input2);

			assertEquals((int)new _ImageQualityTest(image, comparison, null).mse, 0);
		}
	}
}
