package org.terifan.imageio.jpeg.test;

import java.util.Random;
import org.terifan.imageio.jpeg.DQTMarkerSegment;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTInteger;
import org.terifan.imageio.jpeg.decoder.IDCTInteger2;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTInteger;
import org.terifan.imageio.jpeg.encoder.QuantizationTable;



public class TestDCT
{
	public static void main(String ... args)
	{
		try
		{
			int[] original = new int[64];
			Random rnd = new Random(1);
			for (int i = 0; i < 64; i++)
			{
				original[i] = (i/8+(i%8))*255/14; //rnd.nextInt(256);
			}

//			DQTMarkerSegment dqt = QuantizationTable.buildQuantTable(100, 8, 8, 0);
//			int[] quantTable = dqt.getTableInt();

			int[] blockEnc1 = original.clone();
			int[] blockEnc2 = original.clone();

			new FDCTFloat().forward(blockEnc1);
			new FDCTInteger().forward(blockEnc2);

//			for (int i = 0; i < 64; i++)
//			{
//				blockEnc1[i] /= quantTable[i];
//				blockEnc2[i] /= quantTable[i];
//			}
//
//			for (int i = 0; i < 64; i++)
//			{
//				blockEnc1[i] *= quantTable[i];
//				blockEnc2[i] *= quantTable[i];
//			}

			int[] blockDec1a = blockEnc1.clone();
			int[] blockDec2a = blockEnc1.clone();
			int[] blockDec3a = blockEnc1.clone();
			int[] blockDec4a = blockEnc1.clone();

			new IDCTFloat().transform(blockDec1a);
			new IDCTInteger().transform(blockDec2a);
			new IDCTInteger2().inverse(blockDec3a);
			new IDCTIntegerFast().transform(blockDec4a);

			int[] blockDec1b = blockEnc2.clone();
			int[] blockDec2b = blockEnc2.clone();
			int[] blockDec3b = blockEnc2.clone();
			int[] blockDec4b = blockEnc2.clone();

			new IDCTFloat().transform(blockDec1b);
			new IDCTInteger().transform(blockDec2b);
			new IDCTInteger2().inverse(blockDec3b);
			new IDCTIntegerFast().transform(blockDec4b);

			printTables(new int[][]{original});
			System.out.println();
			printTables(new int[][]{blockEnc1, blockEnc2});
			System.out.println();
			printTables(new int[][]{blockDec1a, blockDec2a, blockDec3a, blockDec4a});
			System.out.println();
			printTables(new int[][]{blockDec1b, blockDec2b, blockDec3b, blockDec4b});
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void printTables(int[][] aInput)
	{
		for (int r = 0; r < 8; r++)
		{
			for (int t = 0; t < aInput.length; t++)
			{
				for (int c = 0; c < 8; c++)
				{
					System.out.printf("%5d ", aInput[t][r*8+c]);
				}
				System.out.print(r == 4 && t < aInput.length-1 ? "  ===>  " : "        ");
			}
			System.out.println();
		}
	}
}
