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
			Path file = Files.createTempFile("shuffleimage", ".jpg"); // Paths.get("d:\\test.jpg")

			System.out.println(file);

			{
				// Load image and extract coefficients, shuffle all MCU:s and save the image to disk. The pin code initilizes the random order.

				int pin = 1;

				JPEG input = new JPEGImageIO().decode(R.class.getResource("Swallowtail.jpg"));

				int[][][][] shuffledCoefficients = shuffle(input.getCoefficients(), true, pin);

				byte[] shuffledImageData = updateAndShowImage(shuffledCoefficients, input);

				Files.write(file, shuffledImageData);
			}

			{
				// Load shuffled image and extract coefficients, restore shuffle MCU:s. An incorrect pin code will result in a bad image.

				int pin = 1;

				byte[] shuffledImageData = Files.readAllBytes(file);

				JPEG input = new JPEGImageIO().decode(shuffledImageData);

				int[][][][] shuffledCoefficients = shuffle(input.getCoefficients(), false, pin);

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
		output.mQuantizationTables = aInput.mQuantizationTables;
		output.mColorSpace = aInput.mColorSpace;
		output.mSOFSegment = aInput.mSOFSegment;
		output.mCoefficients = aCoefficients;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		new JPEGImageIO().encode(output, baos);

		_ImageWindow.show(new JPEGImageIO().read(baos.toByteArray()));

		return baos.toByteArray();
	}


	private static int[][][][] shuffle(int[][][][] aCoefficients, boolean aEncode, int aPinCode)
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
}
