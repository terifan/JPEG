package org.terifan.imageio.jpeg.test;

import java.io.File;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.encoder.JPEGImageIO;


public class TestBatch
{
	public static void main(String... args)
	{
		try
		{
			JPEGConstants.VERBOSE = true;

			for (File dir : new File("D:\\Pictures").listFiles(e->e.isDirectory()))
			{
				for (File file : dir.listFiles(e->e.getName().toLowerCase().endsWith(".jpg") && e.length() < 10000000))
				{
					System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
					System.out.println("-- " + file + " ---------------------------------------------------------------------------------------------------------------------------------------------------------");
					System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

					try
					{
						new JPEGImageIO().read(file);
					}
					catch (Throwable e)
					{
						e.printStackTrace(System.out);
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
