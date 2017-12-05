package org.terifan.imageio.jpeg.test;

import java.util.Random;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;



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

			int[] enc = original.clone();
			new FDCTIntegerSlow().transform(enc);
//			new FDCTIntegerFast().transform(enc);
//			new FDCTFloat().transform(enc);
			int[] dec = enc.clone();
			new IDCTIntegerFast().transform(dec);
			printTables(new int[][]{original,enc,dec});


//			String[] s = "-468,16,0,0,-1,-1,0,0,28,3,2,0,0,0,0,0,0,1,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0".split(",");
//
//			DQTMarkerSegment qt = new DQTMarkerSegment(0, 64,44,42,75,96,126,104,62,44,62,116,104,133,244,168,86,84,116,109,147,209,230,181,81,75,104,147,133,226,255,204,73,64,133,167,255,255,255,208,79,75,139,230,237,251,237,190,76,104,192,204,204,208,204,131,57,79,135,127,125,115,83,57,29);
//
//			int[] enc = new int[64];
//			for (int i = 0; i < 64;i++) enc[i]=Integer.parseInt(s[i]);
//
//			int[] dec1 = enc.clone();
//			int[] dec2 = enc.clone();
//			int[] dec3 = enc.clone();
//			int[] dec4 = enc.clone();
//
//			new IDCTIntegerFast().transform(dec1, qt);
//			new IDCTFloat().transform(dec2, qt);
//			new IDCTIntegerSlow().transform(dec3, qt);
//			new IDCTInteger2().transform(dec4, qt);
//
//			printTables(new int[][]{enc,dec1,dec2,dec3,dec4});
//			
//			for (int i = 0; i < 64; i++) dec2[i]=Math.abs(dec2[i]-dec1[i]);
//			for (int i = 0; i < 64; i++) dec3[i]=Math.abs(dec3[i]-dec1[i]);
//			for (int i = 0; i < 64; i++) dec4[i]=Math.abs(dec4[i]-dec1[i]);
//			
//			System.out.println();
//			printTables(new int[][]{dec2,dec3,dec4});
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
