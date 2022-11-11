package examples;

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
public class ShuffleCoefficientDemo
{
	public static void main(String... args)
	{
		try
		{
//			Path destinationFile = Files.createTempFile("shuffleimage", ".jpg");
			Path destinationFile = Paths.get("d:\\shuffleimage.jpg");

			System.out.println(destinationFile);

			boolean shuffleAC = true;
			boolean shuffleDC = !true;

			{
				// Load image and extract coefficients, shuffle all MCU:s and save the image to disk. The pin code initilizes the random order.

				int pin = 1;

//				JPEG input = new JPEGImageIO().decode(R.class.getResource("Swallowtail.jpg"));
//				JPEG input = new JPEGImageIO().decode("D:\\Pictures\\bztizllhrpq91.jpg");
				JPEG input = new JPEGImageIO().decode("D:\\Pictures\\wallpapers\\t3_6nyw6m.jpg");

				int[][][][] shuffledCoefficients = shuffleBlock(input.getCoefficients(), true, pin, shuffleAC, shuffleDC);

				byte[] shuffledImageData = updateAndShowImage(shuffledCoefficients, input);

				Files.write(destinationFile, shuffledImageData);
			}

			{
				// Load shuffled image and extract coefficients, restore shuffle MCU:s. An incorrect pin code will result in a bad image.

				int pin = 1;

				byte[] shuffledImageData = Files.readAllBytes(destinationFile);

				JPEG input = new JPEGImageIO().decode(shuffledImageData);

				int[][][][] shuffledCoefficients = shuffleBlock(input.getCoefficients(), false, pin, shuffleAC, shuffleDC);

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


	private static int[][][][] shuffleBlock(int[][][][] aCoefficients, boolean aEncode, int aPinCode, boolean aShuffleAC, boolean aShuffleDC)
	{
		int rows = aCoefficients.length;
		int cols = aCoefficients[0].length;
		int mcus = aCoefficients[0][0].length;
		int[][][][] outCoefficients = aCoefficients.clone();

		ArrayList<Position> fromListAC = new ArrayList<>();
		ArrayList<Position> toListAC = new ArrayList<>();
		ArrayList<Position> fromListDC = new ArrayList<>();
		ArrayList<Position> toListDC = new ArrayList<>();

		for (int row = 10; row < rows - 10; row++)
		{
			outCoefficients[row] = aCoefficients[row].clone();
			for (int col = 10; col < cols - 10; col++)
			{
				outCoefficients[row][col] = aCoefficients[row][col].clone();
				for (int mcu = 0; mcu < mcus; mcu++)
				{
					outCoefficients[row][col][mcu] = aCoefficients[row][col][mcu].clone();
					for (int coef = 1; coef < 64; coef++)
					{
						fromListAC.add(new Position(row, col, mcu, coef));
						toListAC.add(new Position(row, col, mcu, coef));
					}
					fromListDC.add(new Position(row, col, mcu, 0));
					toListDC.add(new Position(row, col, mcu, 0));
				}
			}
		}

		Random rnd = new Random(aPinCode);

		if (aShuffleAC)
		{
			Collections.shuffle(aEncode ? toListAC : fromListAC, rnd);
			for (int i = 0; i < fromListAC.size(); i++)
			{
				Position from = fromListAC.get(i);
				Position to = toListAC.get(i);
				outCoefficients[to.row][to.col][to.mcu][to.coef] = aCoefficients[from.row][from.col][from.mcu][from.coef];
			}
		}

		if (aShuffleDC)
		{
			Collections.shuffle(aEncode ? toListDC : fromListDC, rnd);
			for (int i = 0; i < fromListDC.size(); i++)
			{
				Position from = fromListDC.get(i);
				Position to = toListDC.get(i);
				outCoefficients[to.row][to.col][to.mcu][to.coef] = aCoefficients[from.row][from.col][from.mcu][from.coef];
			}
		}

		return outCoefficients;
	}


	private record Position(int row, int col, int mcu, int coef)
		{
	}
}
