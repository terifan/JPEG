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

			int numcu = w * h * d;

			int[] order = new int[numcu];
			int[] reorder = new int[numcu];
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
				int srcO = i;
				int si = srcO % w;
				int sj = (srcO / w) % h;
				int sk = (srcO / w / h) % d;

				int dstO = order[i];
				int di = dstO % w;
				int dj = (dstO / w) % h;
				int dk = (dstO / w / h) % d;

				dstCoefficients[di][dj][dk] = srcCoefficients[si][sj][sk];
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
