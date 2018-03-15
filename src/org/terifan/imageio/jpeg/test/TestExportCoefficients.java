package org.terifan.imageio.jpeg.test;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;


public class TestExportCoefficients
{
//	public static void main(String... args)
//	{
//		try
//		{
//			for (File file : new File("D:\\Pictures\\Image Compression Suit").listFiles())
//			{
//				BufferedImage img = ImageIO.read(file);
//
//				BufferedImage dest = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
//				Graphics2D g = dest.createGraphics();
//				g.drawImage(img, 0, 0, null);
//				g.dispose();
//				
//				float quality = 0.95f;
//				
//				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//
//				ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
//				ImageWriteParam iwp = writer.getDefaultWriteParam();
//				iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//				iwp.setCompressionQuality(quality);
//				writer.setOutput(ImageIO.createImageOutputStream(buffer));
//				writer.write(null, new IIOImage(dest, null, null), iwp);
//				writer.dispose();
//
//				try (FileOutputStream fos = new FileOutputStream(new File("d:\\temp\\x", file.getName().replace(".png", "-ari.jpg"))))
//				{
//					new Transcode().setArithmetic(true).transcode(new ByteArrayInputStream(buffer.toByteArray()), fos);
//				}
//
//				try (FileOutputStream fos = new FileOutputStream(new File("d:\\temp\\x", file.getName().replace(".png", "-huff.jpg"))))
//				{
//					new Transcode().setOptimizedHuffman(true).transcode(new ByteArrayInputStream(buffer.toByteArray()), fos);
//				}
//				
//				
////				for (String type : new String[]{"-ari", "-ari-prog", "-huff", "-huff-prog"})
////				{
////					FileOutputStream fos = new FileOutputStream(new File("d:\\temp\\x", file.getName().replace(".png", type + ".jpg")));
////					fos.write(buffer.toByteArray());
////					fos.close();
////				}
//			}
//
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}

	
	public static void main(String... args)
	{
		try
		{
			for (File file : new File("D:\\temp\\y").listFiles(e->e.getName().endsWith(".data")))
			{
				File file1 = new File(file.getAbsolutePath().replace(".jpg.data", "-ari.jpg"));
				File file2 = new File(file.getAbsolutePath().replace(".jpg.data", "-ari-prog.jpg"));
				File file3 = new File(file.getAbsolutePath().replace(".jpg.data", "-huff.jpg"));
				File file4 = new File(file.getAbsolutePath().replace(".jpg.data", "-huff-prog.jpg"));

				System.out.println("map.put(\"" + file.getName() + "\", new int[]{"
					+ "" + (file1.length() - findDataOffset(file1))
					+ ", " + (file2.length() - findDataOffset(file2))
					+ ", " + (file3.length() - findDataOffset(file3))
					+ ", " + (file4.length() - findDataOffset(file4))
					+ "});"
				);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.err);
		}
	}

	
	private static int findDataOffset(File aFile) throws IOException
	{
		int o = 0;
		try (FileInputStream in = new FileInputStream(aFile))
		{
			int p = 0;
			for (;;)
			{
				int b = in.read();
				if (p == 0xff && b == 0xda)
				{
					break;
				}
				p = b;
				o++;
			}
		}
		return o;
	}
	

	public static void xmain(String... args)
	{
		try
		{
			for (File file : new File("D:\\temp\\y").listFiles(e->e.getName().endsWith("-ari.jpg")))
			{
				try (FileInputStream in = new FileInputStream(file))
				{
					JPEG jpeg = new JPEGImageReader(in).decode();

					int[][][][] coefficients = jpeg.getCoefficients();

					try (DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(new FileOutputStream("d:\\temp\\y\\" + file.getName().replace("-ari.jpg", ".jpg") + ".data"))))
					{
						dos.writeShort(jpeg.getCoefficients().length);
						dos.writeShort(jpeg.getCoefficients()[0].length);
						dos.writeShort(jpeg.getCoefficients()[0][0].length);

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
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
