package org.terifan.imageio.jpeg;


public class JPEGConstants
{
	public final static boolean VERBOSE = false;

	public final static int[] ZIGZAG_ORDER =
	{
		0, 1, 8, 16, 9, 2, 3, 10,
		17, 24, 32, 25, 18, 11, 4, 5,
		12, 19, 26, 33, 40, 48, 41, 34,
		27, 20, 13, 6, 7, 14, 21, 28,
		35, 42, 49, 56, 57, 50, 43, 36,
		29, 22, 15, 23, 30, 37, 44, 51,
		58, 59, 52, 45, 38, 31, 39, 46,
		53, 60, 61, 54, 47, 55, 62, 63
	};

	public final static int[] NATURAL_ORDER =
	{
		0, 1, 8, 16, 9, 2, 3, 10,
		17, 24, 32, 25, 18, 11, 4, 5,
		12, 19, 26, 33, 40, 48, 41, 34,
		27, 20, 13, 6, 7, 14, 21, 28,
		35, 42, 49, 56, 57, 50, 43, 36,
		29, 22, 15, 23, 30, 37, 44, 51,
		58, 59, 52, 45, 38, 31, 39, 46,
		53, 60, 61, 54, 47, 55, 62, 63
	};

	public final static int APP0 = 0xFFE0;   // JFIF APP0 segment marker
	public final static int APP1 = 0xFFE1;
	public final static int APP2 = 0xFFE2;
	public final static int APP3 = 0xFFE3;
	public final static int APP4 = 0xFFE4;
	public final static int APP5 = 0xFFE5;
	public final static int APP6 = 0xFFE6;
	public final static int APP7 = 0xFFE7;
	public final static int APP8 = 0xFFE8;
	public final static int APP9 = 0xFFE9;
	public final static int APP10 = 0xFFEA;
	public final static int APP11 = 0xFFEB;
	public final static int APP12 = 0xFFEC;
	public final static int APP13 = 0xFFED;
	public final static int APP14 = 0xFFEE;
	public final static int APP15 = 0xFFEF;
	public final static int COM = 0xFFFE;   // Comment
	public final static int DAC = 0xFFCC;   // Define Arithmetic Table, usually unsupported
	public final static int DHP = 0xFFDE;   // Reserved, fatal error
	public final static int DHT = 0xFFC4;   // Define Huffman Table
	public final static int DNL = 0xFFDC;
	public final static int DQT = 0xFFDB;   // Define Quantization Table
	public final static int DRI = 0xFFDD;   // Define Restart Interval
	public final static int EOI = 0xFFD9;   // End Of Image
	public final static int EXP = 0xFFDF;   // Reserved, fatal error
	public final static int JPG = 0xFFC8;   // Reserved, fatal error
	public final static int JPG0 = 0xFFF0;   // Reserved, fatal error
	public final static int JPG13 = 0xFFFD;   // Reserved, fatal error
	public final static int RST0 = 0xFFD0;   // RSTn are used for resync
	public final static int RST1 = 0xFFD1;
	public final static int RST2 = 0xFFD2;
	public final static int RST3 = 0xFFD3;
	public final static int RST4 = 0xFFD4;
	public final static int RST5 = 0xFFD5;
	public final static int RST6 = 0xFFD6;
	public final static int RST7 = 0xFFD7;
	public final static int SOF0 = 0xFFC0;   // Baseline
	public final static int SOF1 = 0xFFC1;   // Extended sequential, Huffman
	public final static int SOF2 = 0xFFC2;   // Progressive, Huffman
	public final static int SOF3 = 0xFFC3;   // Unsupported; Lossless, Huffman
	public final static int SOF5 = 0xFFC5;   // Unsupported; Differential sequential, Huffman
	public final static int SOF6 = 0xFFC6;   // Unsupported; Differential progressive, Huffman
	public final static int SOF7 = 0xFFC7;   // Unsupported; Differential lossless, Huffman
	public final static int SOF9 = 0xFFC9;   // Extended sequential, arithmetic
	public final static int SOF10 = 0xFFCA;   // Progressive, arithmetic
	public final static int SOF11 = 0xFFCB;   // Unsupported; Lossless, Unsupported; arithmetic
	public final static int SOF13 = 0xFFCD;   // Unsupported; Differential sequential, arithmetic
	public final static int SOF14 = 0xFFCE;   // Unsupported; Differential progressive, arithmetic
	public final static int SOF15 = 0xFFCF;   // Unsupported; Differential lossless, arithmetic
	public final static int SOI = 0xFFD8;   // Start Of Image
	public final static int SOS = 0xFFDA;   // Start Of Scan


	public static String getSOFDescription(int aMarker)
	{
		switch (aMarker)
		{
			case SOF0:
				return "Baseline";
			case SOF1:
				return "Extended sequential, Huffman";
			case SOF2:
				return "Progressive, Huffman";
			case SOF3:
				return "Lossless, Huffman";
			case SOF5:
				return "Differential sequential, Huffman";
			case SOF6:
				return "Differential progressive, Huffman";
			case SOF7:
				return "Differential lossless, Huffman";
			case SOF9:
				return "Extended sequential, arithmetic coding";
			case SOF10:
				return "Progressive, arithmetic coding";
			case SOF11:
				return "Lossless, arithmetic coding";
			case SOF13:
				return "Differential sequential, arithmetic coding";
			case SOF14:
				return "Differential progressive, arithmetic coding";
			case SOF15:
				return "Differential lossless, arithmetic coding";
			case APP0:
				return "APP0";
			case APP1:
				return "APP1";
			case APP2:
				return "APP2";
			case APP3:
				return "APP3";
			case APP4:
				return "APP4";
			case APP5:
				return "APP5";
			case APP6:
				return "APP6";
			case APP7:
				return "APP7";
			case APP8:
				return "APP8";
			case APP9:
				return "APP9";
			case APP10:
				return "APP10";
			case APP11:
				return "APP11";
			case APP12:
				return "APP12";
			case APP13:
				return "APP13";
			case APP14:
				return "APP14";
			case APP15:
				return "APP15";
			case COM:
				return "Comment";
			case DAC:
				return "Define Arithmetic Table";
			case DHP:
				return "DHP";
			case DHT:
				return "Define Huffman Table";
			case DNL:
				return "DNL";
			case DQT:
				return "Define Quantization Table";
			case DRI:
				return "Define Restart Interval";
			case EOI:
				return "End Of Image";
			case EXP:
				return "EXP";
			case JPG:
				return "JPG";
			case JPG0:
				return "JPG0";
			case JPG13:
				return "JPG13";
			case RST0:
				return "RST0";
			case RST1:
				return "RST1";
			case RST2:
				return "RST2";
			case RST3:
				return "RST3";
			case RST4:
				return "RST4";
			case RST5:
				return "RST5";
			case RST6:
				return "RST6";
			case RST7:
				return "RST7";
			case SOI:
				return "Start Of Image";
			case SOS:
				return "Start Of Scan";
			default:
				return "Unknown";
		}
	}
}
