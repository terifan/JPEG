package org.terifan.imageio.jpeg.test;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.DeflaterOutputStream;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGImageIO;


public class TestExportCoefficients
{
	public static void main(String... args)
	{
		try
		{
			for (File file : new File("D:\\temp\\x").listFiles(e->e.getName().endsWith("-ari.jpg")))
			{
				JPEGImageIO reader = new JPEGImageIO();
				JPEG jpeg = reader.decode(file);

				int[][][][] coefficients = jpeg.getCoefficients();

				try (DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(new FileOutputStream("d:\\temp\\x\\" + file.getName().replace("-ari.jpg", ".jpg") + ".data"))))
				{
					dos.writeShort(coefficients.length);
					dos.writeShort(coefficients[0].length);
					dos.writeShort(coefficients[0][0].length);

					for (int mcuY = 0; mcuY < coefficients.length; mcuY++)
					{
						for (int mcuX = 0; mcuX < coefficients[mcuY].length; mcuX++)
						{
							for (int blockIndex = 0; blockIndex < coefficients[mcuY][mcuX].length; blockIndex++)
							{
								for (int i = 0; i < coefficients[mcuY][mcuX][blockIndex].length; i++)
								{
									dos.writeShort(coefficients[mcuY][mcuX][blockIndex][i]);

//									System.out.printf("%5d ", coefficients[mcuY][mcuX][blockIndex][i]);
//									if ((i % 8) == 7) System.out.println();
								}
//								System.out.println();
							}
//							System.out.println();
						}
					}
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
