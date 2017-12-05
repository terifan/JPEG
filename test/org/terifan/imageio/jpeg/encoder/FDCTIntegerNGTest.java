package org.terifan.imageio.jpeg.encoder;

import java.util.Random;
import org.terifan.imageio.jpeg.DQTMarkerSegment;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class FDCTIntegerNGTest
{
	@Test
	public void testSomeMethod()
	{
		int[] block = new int[64];
		Random rnd = new Random(1);
		for (int i = 0; i < 64; i++)
		{
			block[i] = (i/8+(i%8))*255/14; //rnd.nextInt(256);
		}

		DQTMarkerSegment dqt = QuantizationTable.buildQuantTable(100, 8, 8, 0);
		int[] quantTable = dqt.getTableInt();

		int[] original = block.clone();

		new FDCTFloat().transform(block);
//		new FDCTInteger().forward(block);

		int[] transformed = block.clone();

		for (int i = 0; i < 64; i++)
		{
			block[i] /= quantTable[i];
		}

		int[] quantizised = block.clone();

		for (int i = 0; i < 64; i++)
		{
			block[i] *= quantTable[i];
		}

		new IDCTFloat().transform(block);
//		new IDCTInteger().transform(block);
//		new FDCTInteger().inverse(block);

		printTables(new int[][]{quantTable, original, transformed, quantizised, block});

		for (int i = 0; i < 64; i++)
		{
			if (Math.abs(original[i] - block[i]) > 1)
			{
				fail();
			}
		}
	}


	private void printTables(int[][] aInput)
	{
		for (int r = 0; r < 8; r++)
		{
			for (int t = 0; t < aInput.length; t++)
			{
				for (int c = 0; c < 8; c++)
				{
					System.out.printf("%4d ", aInput[t][r*8+c]);
				}
				System.out.print(r == 4 && t < aInput.length-1 ? "  ===>  " : "        ");
			}
			System.out.println();
		}
	}
}
