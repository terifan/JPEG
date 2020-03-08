package examples;

import examples.res.R;
import java.io.ByteArrayOutputStream;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGImageIO;


/**
 * Lossless reordering of blocks in a JPEG.
 */
public class CoefficientAccessDemo
{
	public static void main(String... args)
	{
		try
		{
			JPEG input = new JPEGImageIO().decode(R.class.getResource("Swallowtail.jpg"));

			for (int loop = 0; loop < 2; loop++)
			{
				int[][][][] coefficients = input.getCoefficients();

				for (int i = 0; i < coefficients.length; i++)
				{
					for (int j = 0; j < coefficients[i].length; j++)
					{
						int[] b0 = coefficients[i][j][0];
						int[] b1 = coefficients[i][j][1];
						int[] b2 = coefficients[i][j][2];
						int[] b3 = coefficients[i][j][3];

						coefficients[i][j][0] = b3;
						coefficients[i][j][1] = b2;
						coefficients[i][j][2] = b1;
						coefficients[i][j][3] = b0;
					}
				}

				JPEG output = new JPEG();
				output.mCoefficients = coefficients;
				output.mColorSpace = input.mColorSpace;
				output.mQuantizationTables = input.mQuantizationTables;
				output.mSOFSegment = input.mSOFSegment;

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				new JPEGImageIO().encode(output, baos);

				_ImageWindow.show(new JPEGImageIO().read(baos.toByteArray()));

				input = output;
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
