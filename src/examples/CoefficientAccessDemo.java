package examples;

import examples.res.R;
import java.io.ByteArrayOutputStream;
import java.util.Random;
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

			int[][][][] srcCoefficients = input.getCoefficients();

			int w = srcCoefficients.length;
			int h = srcCoefficients[0].length;
			int d = srcCoefficients[0][0].length;

			int numcu = w;
			int[] order = new int[numcu];
			for (int i = 0; i < numcu; i++)
			{
				order[i] = i;
			}

			Random rnd = new Random();
			for (int i = 0; i < numcu; i++)
			{
				int j = rnd.nextInt(numcu);
				int tmp = order[j];
				order[j] = order[i];
				order[i] = tmp;
			}

			int[][][][] dstCoefficients = new int[w][h][d][];
			for (int i = 0; i < numcu; i++)
			{
				dstCoefficients[i] = srcCoefficients[order[i]];
			}



			numcu = h;
			order = new int[numcu];
			for (int i = 0; i < numcu; i++)
			{
				order[i] = i;
			}

			for (int i = 0; i < numcu; i++)
			{
				int j = rnd.nextInt(numcu);
				int tmp = order[j];
				order[j] = order[i];
				order[i] = tmp;
			}

			srcCoefficients = dstCoefficients;
			dstCoefficients = new int[w][h][d][];

			for (int i = 0; i < w; i++)
			{
				for (int j = 0; j < numcu; j++)
				{
					dstCoefficients[i][j] = srcCoefficients[i][order[j]];
				}
			}



			numcu = d;
			order = new int[numcu];
			for (int i = 0; i < numcu; i++)
			{
				order[i] = i;
			}

			for (int i = 0; i < 4; i++)
			{
				int j = rnd.nextInt(4);
				int tmp = order[j];
				order[j] = order[i];
				order[i] = tmp;
			}

			srcCoefficients = dstCoefficients;
			dstCoefficients = new int[w][h][d][];

			for (int i = 0; i < w; i++)
			{
				for (int j = 0; j < h; j++)
				{
					for (int k = 0; k < numcu; k++)
					{
						dstCoefficients[i][j][k] = srcCoefficients[i][j][order[k]];
					}
				}
			}



			JPEG output = new JPEG();
			output.mCoefficients = dstCoefficients;
			output.mColorSpace = input.mColorSpace;
			output.mQuantizationTables = input.mQuantizationTables;
			output.mSOFSegment = input.mSOFSegment;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			new JPEGImageIO().encode(output, baos);

			_ImageWindow.show(new JPEGImageIO().read(baos.toByteArray()));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
