package org.terifan.imageio.jpeg.test;

import java.util.Random;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerXX;
import org.terifan.imageio.jpeg.decoder.IDCTInteger2;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTInteger;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerXX;



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

			int[][] blockEnc = {original.clone(), original.clone(), original.clone()};

			new FDCTFloat().forward(blockEnc[0]);
			new FDCTIntegerXX().forward(blockEnc[1]);
			new FDCTInteger().forward(blockEnc[2]);

			int[][][] blockDec = new int[blockEnc.length][4][];
			for (int i = 0; i < blockEnc.length; i++)
			{
				for (int j = 0; j < 4; j++)
				{
					blockDec[i][j] = blockEnc[i].clone();
				}

				new IDCTFloat().transform(blockDec[i][0]);
				new IDCTIntegerXX().transform(blockDec[i][1]);
				new IDCTInteger2().inverse(blockDec[i][2]);
				new IDCTIntegerFast().transform(blockDec[i][3]);
			}

			printTables(new int[][]{original});
			System.out.println();
			printTables(blockEnc);
			System.out.println();
			printTables(blockDec[0]);
			System.out.println();
			printTables(blockDec[1]);
			System.out.println();
			printTables(blockDec[2]);
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
