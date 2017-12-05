package org.terifan.imageio.jpeg.test;

import java.util.Random;
import org.terifan.imageio.jpeg.DQTMarkerSegment;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.decoder.IDCTInteger2;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
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

//			int[][] blockEnc = {original.clone(), original.clone(), original.clone()};
//
//			new FDCTFloat().forward(blockEnc[0]);
//			new FDCTIntegerSlow().forward(blockEnc[1]);
//			new FDCTInteger().forward(blockEnc[2]);
//
//			int[][][] blockDec = new int[blockEnc.length][4][];
//			for (int i = 0; i < blockEnc.length; i++)
//			{
//				for (int j = 0; j < 4; j++)
//				{
//					blockDec[i][j] = blockEnc[i].clone();
//				}
//
//				new IDCTFloat().transform(blockDec[i][0]);
//				new IDCTIntegerSlow().transform(blockDec[i][1]);
//				new IDCTInteger2().inverse(blockDec[i][2]);
//				new IDCTIntegerFast().transform(blockDec[i][3]);
//			}
//
//			printTables(new int[][]{original});
//			System.out.println();
//			printTables(blockEnc);
//			System.out.println();
//			printTables(blockDec[0]);
//			System.out.println();
//			printTables(blockDec[1]);
//			System.out.println();
//			printTables(blockDec[2]);

//			int[] enc = original.clone();
//			new FDCTIntegerSlow().forward(enc);
//			int[] dec = enc.clone();
//			new IDCTIntegerSlow().transform(dec);
//			printTables(new int[][]{original,enc,dec});

			DQTMarkerSegment q = QuantizationTable.buildQuantTable(75, 8, 8, 1);

String s = (
" -378   -87   -20     0     2    -1     1     0         \n" +
"  -44    24    -1     8    -3     0     0     0         \n" +
"   -5     2    10    -4     1     1    -1     1         \n" +
"    0     4     0     0     1     0     0     0         \n" +
"    7    -2     0     1     0     0     0     0         \n" +
"    1     0     0     0     0     0     0     0         \n" +
"    0     0     0     0     0     0     0     0         \n" +
"    0     0     0     0     0     0     0     0  " ).replace("\n","").replace("    "," ").replace("    "," ").replace("   "," ").replace("  "," ").trim();

			int[] enc = new int[64];
			for (int i = 0; i < 64;i++) enc[i]=Integer.parseInt(s.split(" ")[i]);

			int[] dec1 = enc.clone();
			int[] dec2 = enc.clone();
			int[] dec3 = enc.clone();
			int[] dec4 = enc.clone();

			new IDCTIntegerSlow().transform(dec1, q);
			new IDCTIntegerFast().transform(dec2, q);
			new IDCTInteger2().transform(dec3, q);
			new IDCTFloat().transform(dec4, q);

			printTables(new int[][]{enc,dec1,dec2,dec3,dec4});
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
					System.out.printf("%6d ", aInput[t][r*8+c]);
				}
				System.out.print(r == 4 && t < aInput.length-1 ? "  ===>" : "      ");
			}
			System.out.println();
		}
	}
}
