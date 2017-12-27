package org.terifan.imageio.jpeg.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;
import org.terifan.imageio.jpeg.Transcode;
import org.terifan.imageio.jpeg.decoder.JPEGImageReader;
import org.terifan.imageio.jpeg.exif.JPEGExif;


public class TestTranscodeBatch
{
	public static void main(String... args)
	{
		try
		{
			int fail = 0;
			int ok = 0;

			for (File dir : new File("D:\\Pictures").listFiles(e->e.isDirectory()))
			{
				for (File file : dir.listFiles(e->e.getName().toLowerCase().endsWith(".jpg") && e.length() < 10000000))
//				for (File file : new File("D:\\Pictures\\Wallpapers High Quality").listFiles())
//				File file = new File("D:\\Pictures\\Wallpapers\\1 (1).jpg");
				{
					byte[] data = new byte[(int)file.length()];
					try (FileInputStream in = new FileInputStream(file))
					{
						in.read(data);
					}

					try
					{
//						System.out.println("-- " + file + " ---------------------------------------------------------------------------------------------------------------------------------------------------------");

						ByteArrayOutputStream ariData = new ByteArrayOutputStream();

						BufferedImage imageHuff = JPEGImageReader.read(new ByteArrayInputStream(data));

//						new Transcode().transcode(new ByteArrayInputStream(data), ariData);
//
//						BufferedImage imageAri = JPEGImageReader.read(new ByteArrayInputStream(ariData.toByteArray()));

						int err = 0;
//						for (int y = 0; y < imageOriginal.getHeight(); y++)
//						{
//							for (int x = 0; x < imageOriginal.getWidth(); x++)
//							{
//								if (imageOriginal.getRGB(x, y) != imageAri.getRGB(x, y))
//								{
//									err++;
//								}
//							}
//						}

//						ImageIO.write(imageAri, "png", new File("D:\\temp\\jpg-test\\" + file.getName().replace("jpg", "png")));

						BufferedImage javaImage = ImageIO.read(file);
						double epp = MeasureErrorRate.measureError(javaImage, imageHuff);
//						double psnr = PSNR.calculate(javaImage, imageAri);
						double psnr = 0;

						System.out.printf("%6.2f %8d %8d %8d %8d %5.2f %s%n", epp, ariData.size(), file.length(), file.length()-ariData.size(), err, psnr, file);

						if (epp > 10)
						{
							try (FileOutputStream fos = new FileOutputStream(new File("D:\\temp\\jpg-err\\" + file.getName())))
							{
								fos.write(data);
							}
						}

//						BufferedImage delta = new BufferedImage(imageAri.getWidth(), imageAri.getHeight(), BufferedImage.TYPE_INT_RGB);
//						for (int y = 0; y < imageAri.getHeight(); y++)
//						{
//							for (int x = 0; x < imageAri.getWidth(); x++)
//							{
//								int c0 = imageAri.getRGB(x, y);
//								int c1 = javaImage.getRGB(x, y);
//								int r0 = 255 & (c0 >> 16);
//								int g0 = 255 & (c0 >> 8);
//								int b0 = 255 & (c0);
//								int r1 = 255 & (c1 >> 16);
//								int g1 = 255 & (c1 >> 8);
//								int b1 = 255 & (c1);
//								int er = Math.abs(r0 - r1);
//								int eg = Math.abs(g0 - g1);
//								int eb = Math.abs(b0 - b1);
//								delta.setRGB(x, y, (er<<16)+(eg<<8)+eb);
//							}
//						}
//						ImageIO.write(delta, "png", new File("D:\\temp\\jpg-delta\\" + file.getName().replace("jpg", "png")));
						
						if (err == 0)
						{
							ok++;

							try (FileOutputStream fos = new FileOutputStream(new File("D:\\temp\\jpg-ari\\" + file.getName())))
							{
								ariData.writeTo(fos);
							}

							data = JPEGExif.replace(data, null);

							try (FileOutputStream fos = new FileOutputStream(new File("D:\\temp\\jpg-huff\\" + file.getName())))
							{
								fos.write(data);
							}
						}
						else
						{
							fail++;
						}
					}
					catch (Throwable e)
					{
						fail++;
						System.out.println("-- " + file + " ---------------------------------------------------------------------------------------------------------------------------------------------------------");
						e.printStackTrace(System.out);
					}
				}
			}

			System.out.println(ok+" "+fail);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
