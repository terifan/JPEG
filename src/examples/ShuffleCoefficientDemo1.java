package examples;

import examples.res.R;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGImageIO;
import org.terifan.imageio.jpeg.JPEGImageIOException;


/**
 * Demonstrates how reordering of blocks in a JPEG is possible.
 */
public class ShuffleCoefficientDemo1
{
	public static void main(String... args)
	{
		try
		{
//			Path destinationFile = Files.createTempFile("shuffleimage", ".jpg");
			Path destinationFile = Paths.get("d:\\shuffleimage.jpg");

			System.out.println(destinationFile);

			{
				JPEG input = new JPEGImageIO().decode(R.class.getResource("Swallowtail.jpg"));
//				JPEG input = new JPEGImageIO().decode("D:\\Pictures\\bztizllhrpq91.jpg");
//				JPEG input = new JPEGImageIO().decode("D:\\Pictures\\wallpapers\\t3_6nyw6m.jpg");

				int[][][][] shuffledCoefficients = shuffleBlock(input.getCoefficients(), true);

				byte[] shuffledImageData = updateAndShowImage(shuffledCoefficients, input);

				Files.write(destinationFile, shuffledImageData);
			}

//			System.out.println(R.class.getResourceAsStream("Swallowtail.jpg").readAllBytes().length);
			System.out.println(Files.size(destinationFile));

			{
				byte[] shuffledImageData = Files.readAllBytes(destinationFile);

				JPEG input = new JPEGImageIO().decode(shuffledImageData);

				int[][][][] shuffledCoefficients = shuffleBlock(input.getCoefficients(), false);

				updateAndShowImage(shuffledCoefficients, input);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static byte[] updateAndShowImage(int[][][][] aCoefficients, JPEG aInput) throws JPEGImageIOException
	{
		JPEG output = new JPEG();
		output.mDQTSegment = aInput.mDQTSegment;
		output.mColorSpace = aInput.mColorSpace;
		output.mSOFSegment = aInput.mSOFSegment;
		output.mCoefficients = aCoefficients;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		new JPEGImageIO().encode(output, baos);

		_ImageWindow.show(new JPEGImageIO().read(baos.toByteArray()));

		return baos.toByteArray();
	}


	private static int[][][][] shuffleBlock(int[][][][] aCoefficients, boolean aEncode)
	{
		int rows = aCoefficients.length;
		int cols = aCoefficients[0].length;
		int mcus = aCoefficients[0][0].length;
		int[][][][] outCoefficients = new int[rows][cols][mcus][64];

		Random rnd = new Random(1);

		ArrayList<Integer> cofList = createList(rnd, 64);
		ArrayList<Integer> rowList = createList(rnd, rows);
		ArrayList<Integer> colList = createList(rnd, cols);
		ArrayList<Integer> mcuList = createList(rnd, mcus);

		int[] xor = {
			200, 100, 50, 25, 12, 6, 3, 3,
			100,  75, 35, 12,  6, 3, 3, 3,
			 50,  35, 20, 12,  6, 3, 3, 3,
			 25,  12, 12, 12,  6, 3, 3, 3,
			 12,   6,  6,  6 , 6, 3, 3, 3,
			  6,   3,  3,  3,  3, 3, 3, 3,
			  3,   3,  3,  3,  3, 3, 3, 3,
			  3,   3,  3,  3,  3, 3, 3, 3,
		};
		int[] ORDER =
		{
			0, 1, 8, 16, 9, 2, 3, 10,
			17, 24, 32, 25, 18, 11, 4, 5,
			12, 19, 26, 33, 40, 48, 41, 34,
			27, 20, 13, 6, 7, 14, 21, 28,
			35, 42, 49, 56, 57, 50, 43, 36,
			29, 22, 15, 23, 30, 37, 44, 51,
			58, 59, 52, 45, 38, 31, 39, 46,
			53, 60, 61, 54, 47, 55, 62, 63
		};

		for (int row = 0; row < rows; row++)
		{
			for (int col = 0; col < cols; col++)
			{
				for (int mcu = 0; mcu < mcus; mcu++)
				{
					for (int cof = 0; cof < 64; cof++)
					{
						int r = rowList.get(row);
						int c = colList.get(col);
						int m = mcuList.get(mcu);
						int f = cof; //cofList.get(cof);
						int z = rnd.nextInt(xor[cof]);
						int s = rnd.nextInt(2) * 2 - 1;

						if (aEncode)
						{
							outCoefficients[row][col][mcu][cof] = s * (z ^ aCoefficients[r][c][m][f]);
						}
						else
						{
							outCoefficients[r][c][m][f] = z ^ (s * aCoefficients[row][col][mcu][cof]);
						}
					}
				}
			}
		}

		return outCoefficients;
	}


	private static ArrayList<Integer> createList(Random aRnd, int aRange)
	{
		ArrayList<Integer> list = new ArrayList<>();
		for (int i = 0; i < aRange; i++)
		{
			list.add(i);
		}
		Collections.shuffle(list, aRnd);
		return list;
	}
}
