package org.terifan.imageio.jpeg.test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.DQTMarkerSegment;
import org.terifan.imageio.jpeg.decoder.IDCTFloat;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerFast;
import org.terifan.imageio.jpeg.decoder.IDCTIntegerSlow;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.encoder.FDCTFloat;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerFast;
import org.terifan.imageio.jpeg.encoder.FDCTIntegerSlow;
import org.terifan.imageio.jpeg.encoder.QuantizationTable;


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

			DQTMarkerSegment qt = QuantizationTable.buildQuantTable(100, 0);

			int[] enc;
			int[] dec;

			System.out.println("\nFDCTIntegerFast");

			enc = original.clone();
			new FDCTIntegerFast().transform(enc, qt);
			dec = enc.clone();
			new IDCTFloat().transform(dec, qt);
			printTables(new int[][]{original,enc,dec,delta(original,dec)});

			System.out.println("\nFDCTFloat");

			enc = original.clone();
			new FDCTFloat().transform(enc, qt);
			dec = enc.clone();
			new IDCTFloat().transform(dec, qt);
			printTables(new int[][]{original,enc,dec,delta(original,dec)});

			System.out.println("\nFDCTIntegerSlow");

			enc = original.clone();
			new FDCTIntegerSlow().transform(enc, qt);
			dec = enc.clone();
			new IDCTFloat().transform(dec, qt);
			printTables(new int[][]{original,enc,dec,delta(original,dec)});

			System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

			System.out.println("\nIDCTIntegerFast");

			enc = original.clone();
			new FDCTFloat().transform(enc, qt);
			dec = enc.clone();
			new IDCTIntegerFast().transform(dec, qt);
			printTables(new int[][]{original, enc, dec,delta(original,dec)});

			System.out.println("\nIDCTFloat");

			enc = original.clone();
			new FDCTFloat().transform(enc, qt);
			dec = enc.clone();
			new IDCTFloat().transform(dec, qt);
			printTables(new int[][]{original, enc, dec,delta(original,dec)});

			System.out.println("\nIDCTIntegerSlow");

			enc = original.clone();
			new FDCTFloat().transform(enc, qt);
			dec = enc.clone();
			new IDCTIntegerSlow().transform(dec, qt);
			printTables(new int[][]{original, enc, dec,delta(original,dec)});


			URL jpegResource = Test.class.getResource("Swallowtail.jpg");

			BufferedImage image1 = JPEGImageReader.read(jpegResource.openStream(), IDCTFloat.class);
			BufferedImage image2 = JPEGImageReader.read(jpegResource.openStream(), IDCTIntegerFast.class);
			BufferedImage image3 = JPEGImageReader.read(jpegResource.openStream(), IDCTIntegerSlow.class);

			BufferedImage image = new BufferedImage(image1.getWidth()*2, image1.getHeight()*2, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.drawImage(image1, 0*image1.getWidth(), 0*image1.getHeight(), null);
			g.drawImage(image2, 1*image1.getWidth(), 0*image1.getHeight(), null);
			g.drawImage(image3, 0*image1.getWidth(), 1*image1.getHeight(), null);
			g.dispose();

			ImageFrame imagePane = new ImageFrame(image);
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
