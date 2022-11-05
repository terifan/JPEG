package examples;

import examples.res.R;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
			Path file = Files.createTempFile("shuffleimage", ".jpg");

			System.out.println(file);

			{
				// Load image and extract coefficients, shuffle all MCU:s and save the image to disk. The pin code initilizes the random order.

				int pin = 1;

				JPEG input = new JPEGImageIO().decode(R.class.getResource("Swallowtail.jpg"));
//				JPEG input = new JPEGImageIO().decode("D:\\Pictures\\bztizllhrpq91.jpg");

//				int[][][][] shuffledCoefficients = shuffleBlock(input.getCoefficients(), true, pin);
				int[][][][] shuffledCoefficients = shuffleMCU(input.getCoefficients(), true, pin);

				byte[] shuffledImageData = updateAndShowImage(shuffledCoefficients, input);

				Files.write(file, shuffledImageData);
			}

			{
				// Load shuffled image and extract coefficients, restore shuffle MCU:s. An incorrect pin code will result in a bad image.

				int pin = 1;

				byte[] shuffledImageData = Files.readAllBytes(file);

				JPEG input = new JPEGImageIO().decode(shuffledImageData);

//				int[][][][] shuffledCoefficients = shuffleBlock(input.getCoefficients(), false, pin);
				int[][][][] shuffledCoefficients = shuffleMCU(input.getCoefficients(), false, pin);

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


	private static int[][][][] shuffleMCU(int[][][][] aCoefficients, boolean aEncode, int aPinCode)
	{
		int rows = aCoefficients.length;
		int cols = aCoefficients[0].length;

		ArrayList<int[]> list = new ArrayList<>();
		for (int row = 0; row < rows; row++)
		{
			for (int col = 0; col < cols; col++)
			{
				list.add(new int[]{row, col});
			}
		}

		Collections.shuffle(list, new Random(aPinCode));

		int[][][][] coefficients = new int[rows][cols][][];
		for (int row = 0, i = 0; row < rows; row++)
		{
			for (int col = 0; col < cols; col++, i++)
			{
				int r = list.get(i)[0];
				int c = list.get(i)[1];
				if (aEncode)
				{
					coefficients[r][c] = aCoefficients[row][col];
				}
				else
				{
					coefficients[row][col] = aCoefficients[r][c];
				}
			}
		}
		return coefficients;
	}


	private static int[][][][] shuffleBlock(int[][][][] aCoefficients, boolean aEncode, int aPinCode)
	{
		int rows = aCoefficients.length;
		int cols = aCoefficients[0].length;
		int mcus = aCoefficients[0][0].length;

		ArrayList<int[]> list = new ArrayList<>();
		for (int row = 0; row < rows; row++)
		{
			for (int col = 0; col < cols; col++)
			{
				for (int mcu = 0; mcu < mcus; mcu++)
				{
					list.add(new int[]{row, col, mcu});
				}
			}
		}

		Collections.shuffle(list, new Random(aPinCode));

		int[][][][] coefficients = new int[rows][cols][mcus][];
		for (int row = 0, i = 0; row < rows; row++)
		{
			for (int col = 0; col < cols; col++)
			{
				for (int mcu = 0; mcu < mcus; mcu++, i++)
				{
					int r = list.get(i)[0];
					int c = list.get(i)[1];
					int m = list.get(i)[2];
					if (aEncode)
					{
						coefficients[r][c][m] = aCoefficients[row][col][mcu];
					}
					else
					{
						coefficients[row][col][mcu] = aCoefficients[r][c][m];
					}
				}
			}
		}
		return coefficients;
	}
}
