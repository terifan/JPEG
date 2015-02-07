package org.terifan.multimedia.jpeg.test;

import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.terifan.io.Streams;
import org.terifan.multimedia.jpeg.JPEGImageReader;
import org.terifan.util.StopWatch;
import org.terifan.util.log.Log;


public class PerformanceTest
{
	public static void main(String[] args)
	{
		try
		{
			byte[] imageData = Streams.fetch("d:/1107.jpg");
			for (;;)
			{
				Log.out.println(terifanImageReader(imageData) + "\t" + javaImageIO(imageData));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(Log.out);
			System.exit(0);
		}
	}


	static String terifanImageReader(byte[] aImageData) throws Exception
	{
		StopWatch stopWatch = new StopWatch();
		JPEGImageReader.read(new ByteArrayInputStream(aImageData));
		stopWatch.stop();

		return stopWatch.toString();
	}


	static String javaImageIO(byte[] aImageData) throws Exception
	{
		StopWatch stopWatch = new StopWatch();
		ImageIO.read(new ByteArrayInputStream(aImageData));
		stopWatch.stop();

		return stopWatch.toString();
	}
}