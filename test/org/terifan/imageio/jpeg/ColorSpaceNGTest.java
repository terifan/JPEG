package org.terifan.imageio.jpeg;

import java.util.Random;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class ColorSpaceNGTest
{
	@Test
	public void testSomeMethod2()
	{
//		int[] rgbIn = new int[100];
//		int[] rgbOut = new int[100];
//		int[] y = new int[100];
//		int[] u = new int[100];
//		int[] v = new int[100];
//
//		Random rnd = new Random(1);
//		for (int i = 0; i < rgbIn.length; i++)
//		{
//			rgbIn[i] = rnd.nextInt() & 0xffffff;
//		}
//
//		for (int i = 0; i < rgbIn.length; i++)
//		{
//			ColorSpace.rgbToYuvFloat(rgbIn, y, u, v);
//		}
//
//		for (int i = 0; i < rgbIn.length; i++)
//		{
////			rgbOut[i] = ColorSpace.yuvToRgbFloat(y, u, v, i);
////			rgbOut[i] = ColorSpace.yuvToRgbLookup(y, u, v, i);
//			rgbOut[i] = ColorSpace.yuvToRgbFP(y, u, v, i);
//		}
//
//		int err = 0;
//
//		for (int i = 0; i < rgbIn.length; i++)
//		{
//			int dr = Math.abs((0xff & (rgbIn[i] >> 0)) - (0xff & (rgbOut[i] >> 0)));
//			int dg = Math.abs((0xff & (rgbIn[i] >> 8)) - (0xff & (rgbOut[i] >> 8)));
//			int db = Math.abs((0xff & (rgbIn[i] >> 16)) - (0xff & (rgbOut[i] >> 16)));
//
//			assertTrue(dr <= 1);
//			assertTrue(dg <= 1);
//			assertTrue(db <= 1);
//
//			err += dr + dg + db;
//		}
//
//		assertTrue(err <= 95);
	}
}
