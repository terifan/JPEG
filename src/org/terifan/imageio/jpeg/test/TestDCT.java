package org.terifan.imageio.jpeg.test;

import java.util.Random;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.QuantizationTableFactory;


public class TestDCT
{
	public static void main(String ... args)
	{
		try
		{
			int[] original = new int[64];
			Random rnd = new Random(7);
			for (int i = 0; i < 64; i++)
			{
				original[i] = (i/8+(i%8))*255/14;
//				original[i] = rnd.nextInt(1<<rnd.nextInt(8));
			}

			QuantizationTable qt = QuantizationTableFactory.buildQuantTable(100, 0);

			int[] enc;
			int[] dec;

			System.out.println("\nFDCTIntegerFast > IDCTFloat");

			enc = original.clone();
			new FDCTIntegerFast().transform(enc, qt);
			dec = enc.clone();
			new IDCTFloat().transform(dec, qt);
			printTables(new int[][]{original,enc,dec,delta(original,dec)});

			System.out.println("\nFDCTFloat > IDCTFloat");

			enc = original.clone();
			new FDCTFloat().transform(enc, qt);
			dec = enc.clone();
			new IDCTFloat().transform(dec, qt);
			printTables(new int[][]{original,enc,dec,delta(original,dec)});

			System.out.println("\nFDCTIntegerSlow > IDCTFloat");

			enc = original.clone();
			new FDCTIntegerSlow().transform(enc, qt);
			dec = enc.clone();
			new IDCTFloat().transform(dec, qt);
			printTables(new int[][]{original,enc,dec,delta(original,dec)});

			System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

			System.out.println("\nFDCTFloat > IDCTIntegerFast");

			enc = original.clone();
			new FDCTFloat().transform(enc, qt);
			dec = enc.clone();
			new IDCTIntegerFast().transform(dec, qt);
			printTables(new int[][]{original, enc, dec,delta(original,dec)});

			System.out.println("\nFDCTFloat > IDCTFloat");

			enc = original.clone();
			new FDCTFloat().transform(enc, qt);
			dec = enc.clone();
			new IDCTFloat().transform(dec, qt);
			printTables(new int[][]{original, enc, dec,delta(original,dec)});

			System.out.println("\nFDCTFloat > IDCTIntegerSlow");

			enc = original.clone();
			new FDCTFloat().transform(enc, qt);
			dec = enc.clone();
			new IDCTIntegerSlow().transform(dec, qt);
			printTables(new int[][]{original, enc, dec,delta(original,dec)});
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static int[] delta(int[] aInput1, int[] aInput2)
	{
		int[] result = new int[64];
		for (int i = 0; i < 64; i++)
		{
			result[i] = Math.abs(aInput1[i] - aInput2[i]);
		}
		return result;
	}


	private static void printTables(int[][] aInput)
	{
		for (int r = 0; r < 8; r++)
		{
			for (int t = 0; t < aInput.length; t++)
			{
				for (int c = 0; c < 8; c++)
				{
					System.out.printf("%6d ", aInput[t][r*8+c]);
				}
				System.out.print(r == 4 && t < aInput.length-1 ? "  ===>" : "      ");
			}
			System.out.println();
		}
	}
}
