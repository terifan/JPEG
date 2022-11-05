package org.terifan.imageio.jpeg;

import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;


public class ColorICCTransform
{
	public static void transform(JPEG aJPEG, JPEGImage aImage)
	{
		if (aJPEG.mAPP2Segment != null && aJPEG.mAPP2Segment.mICCProfile != null)
		{
			int profileClass = aJPEG.mAPP2Segment.mICCProfile.getProfileClass();

			if ((profileClass != ICC_Profile.CLASS_INPUT) && (profileClass != ICC_Profile.CLASS_DISPLAY) && (profileClass != ICC_Profile.CLASS_OUTPUT) && (profileClass != ICC_Profile.CLASS_COLORSPACECONVERSION) && (profileClass != ICC_Profile.CLASS_NAMEDCOLOR) && (profileClass != ICC_Profile.CLASS_ABSTRACT))
			{
//				reportError("Failed to perform color transform: Invalid profile type");
				return;
			}

//			sun.java2d.cmm.PCMM module = sun.java2d.cmm.CMSManager.getModule();
//
//			sun.java2d.cmm.ColorTransform[] transformList = {
//				module.createTransform(mJPEG.mICCProfile, sun.java2d.cmm.ColorTransform.Any, sun.java2d.cmm.ColorTransform.In),
//				module.createTransform(ICC_Profile.getInstance(ICC_ColorSpace.CS_sRGB), sun.java2d.cmm.ColorTransform.Any, sun.java2d.cmm.ColorTransform.Out)
//			};
//
//			module.createTransform(transformList).colorConvert(image, image);

			java.awt.color.ColorSpace colorSpace = new ICC_ColorSpace(aJPEG.mAPP2Segment.mICCProfile);

			float[] colorvalue = new float[3];

			for (int y = 0; y < aImage.getHeight(); y++)
			{
				for (int x = 0; x < aImage.getWidth(); x++)
				{
					int rgb = aImage.getRGB(x, y);
					colorvalue[0] = (0xff & (rgb >> 16)) / 255f;
					colorvalue[1] = (0xff & (rgb >> 8)) / 255f;
					colorvalue[2] = (0xff & (rgb >> 0)) / 255f;

					colorvalue = colorSpace.toRGB(colorvalue);

					int r = (int)(255f * colorvalue[0] + 0.5f);
					int g = (int)(255f * colorvalue[1] + 0.5f);
					int b = (int)(255f * colorvalue[2] + 0.5f);
					aImage.setRGB(x, y, (r << 16) + (g << 8) + b);
				}
			}
		}
	}
}
