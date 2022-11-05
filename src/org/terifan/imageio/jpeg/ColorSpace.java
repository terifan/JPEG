package org.terifan.imageio.jpeg;

import java.io.Serializable;


//	JCS_RGB,		/* red/green/blue, standard RGB (sRGB) */
//	JCS_YCbCr,		/* Y/Cb/Cr (also known as YUV), standard YCC */
//	JCS_CMYK,		/* C/M/Y/K */
//	JCS_YCCK,		/* Y/Cb/Cr/K */
//	JCS_BG_RGB,		/* big gamut red/green/blue, bg-sRGB */
//	JCS_BG_YCC		/* big gamut Y/Cb/Cr, bg-sYCC */
public interface ColorSpace extends Serializable
{
	void configureImageBuffer(SOFSegment aSOFSegment, JPEGImage aImage);


	void encode(int[] aInput, int[] aY, int[] aCb, int[] aCr);


	int decode(int aY, int aCb, int aCr);


	String getName();
}
