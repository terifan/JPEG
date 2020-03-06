package org.terifan.imageio.jpeg.encoder;

import java.util.Random;
import org.terifan.imageio.jpeg.DQTSegment;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class FDCTIntegerNGTest
{
	@Test
	public void testSomeMethod()
	{
		int[] block = new int[64];
		for (int i = 0; i < 64; i++)
		{
			block[i] = (i/8+(i%8))*255/14;
		}

		QuantizationTable qt = QuantizationTableFactory.buildQuantTable(95, 0);

		int[] original = block.clone();

		new FDCTFloat().transform(block, qt);

		new IDCTFloat().transform(block, qt);

		for (int i = 0; i < 64; i++)
		{
			if (Math.abs(original[i] - block[i]) > 1)
			{
				fail();
			}
		}
	}
}
