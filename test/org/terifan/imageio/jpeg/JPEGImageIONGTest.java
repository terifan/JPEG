package org.terifan.imageio.jpeg;

import examples._ImageQualityTest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class JPEGImageIONGTest
{
	@DataProvider
	public Object[][] getFiles()
	{
		File[] files = new File("c:\\pictures\\Image Compression Suit").listFiles();
		Object[][] params = new Object[files.length][1];
		for (int i = 0; i < files.length; i++)
		{
			params[i][0] = files[i];
		}
		return params;
	}


	@Test(dataProvider = "getFiles")
	public void testEncodeDecodeBatchOfImages(File aFile)
	{
		try
		{
			BufferedImage source = ImageIO.read(aFile);

			for (CompressionType ct : CompressionType.values())
			{
				System.out.println(aFile + " " + ct);

				ByteArrayOutputStream output = new ByteArrayOutputStream();

				new JPEGImageIO().setQuality(100).setSubsampling(SubsamplingMode._444).setCompressionType(ct).write(source, output);

				BufferedImage decoded1 = new JPEGImageIO().read(output.toByteArray());

				assertTrue(new _ImageQualityTest(source, decoded1, null).mse <= 1);

				if (ct != CompressionType.Arithmetic && ct != CompressionType.ArithmeticProgressive)
				{
					BufferedImage decoded2 = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));

					assertTrue(new _ImageQualityTest(decoded1, decoded2, null).mse <= 1);
				}
			}

			assertTrue(true);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
			fail("error", e);
		}
	}
}
