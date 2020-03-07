package org.terifan.imageio.jpeg;


public enum SegmentMarker
{
	SOF0(0xFFC0, "Baseline"),
	SOF1(0xFFC1, "Extended sequential, Huffman"),
	SOF2(0xFFC2, "Progressive, Huffman"),
	DHT(0xFFC4, "Define Huffman Table"),
	JPG(0xFFC8, "Reserved, fatal error"),
	SOF9(0xFFC9, "Sequential, arithmetic"),
	SOF10(0xFFCA, "Progressive, arithmetic"),
	DAC(0xFFCC, "Define Arithmetic Table"),
	SOF3(0xFFC3, "Unsupported; Lossless, Huffman"),
	SOF5(0xFFC5, "Unsupported; Differential sequential, Huffman"),
	SOF6(0xFFC6, "Unsupported; Differential progressive, Huffman"),
	SOF7(0xFFC7, "Unsupported; Differential lossless, Huffman"),
	SOF11(0xFFCB, "Unsupported; Lossless, arithmetic"),
	SOF13(0xFFCD, "Unsupported; Differential sequential, arithmetic"),
	SOF14(0xFFCE, "Unsupported; Differential progressive, arithmetic"),
	SOF15(0xFFCF, "Unsupported; Differential lossless, arithmetic"),

	RST0(0xFFD0), // RSTn are used for resync
	RST1(0xFFD1),
	RST2(0xFFD2),
	RST3(0xFFD3),
	RST4(0xFFD4),
	RST5(0xFFD5),
	RST6(0xFFD6),
	RST7(0xFFD7),
	SOI(0xFFD8, "Start Of Image"),
	EOI(0xFFD9, "End Of Image"),
	SOS(0xFFDA, "Start Of Scan"),
	DQT(0xFFDB, "Define Quantization Table"),
	DNL(0xFFDC, "Number Of Lines"),
	DRI(0xFFDD, "Define Restart Interval"),
	DHP(0xFFDE, "Define Hiearchical Progression"),
	EXP(0xFFDF, "Expands Reference Component(s)"),

	APP0(0xFFE0, "JFIF APP0 segment marker"),
	APP1(0xFFE1, "Exif metadata"),
	APP2(0xFFE2, "FlashPix data"),
	APP3(0xFFE3),
	APP4(0xFFE4),
	APP5(0xFFE5),
	APP6(0xFFE6),
	APP7(0xFFE7),
	APP8(0xFFE8),
	APP9(0xFFE9),
	APP10(0xFFEA),
	APP11(0xFFEB),
	APP12(0xFFEC),
	APP13(0xFFED, "XML metadata"),
	APP14(0xFFEE, "Adobe color profile"),
	APP15(0xFFEF),

	JPG0(0xFFF0),   // Reserved for JPEG extensions, fatal error
	JPG1(0xFFF1),   // Reserved for JPEG extensions, fatal error
	JPG2(0xFFF2),   // Reserved for JPEG extensions, fatal error
	JPG3(0xFFF3),   // Reserved for JPEG extensions, fatal error
	JPG4(0xFFF4),   // Reserved for JPEG extensions, fatal error
	JPG5(0xFFF5),   // Reserved for JPEG extensions, fatal error
	JPG6(0xFFF6),   // Reserved for JPEG extensions, fatal error
	JPG7(0xFFF7),   // Reserved for JPEG extensions, fatal error
	LSE(0xFFF8),    // LSE inverse color transform specification marker
	JPG9(0xFFF9),   // Reserved for JPEG extensions, fatal error
	JPG10(0xFFFA),  // Reserved for JPEG extensions, fatal error
	JPG11(0xFFFB),  // Reserved for JPEG extensions, fatal error
	JPG12(0xFFFC),  // Reserved for JPEG extensions, fatal error
	JPG13(0xFFFD),  // Reserved for JPEG extensions, fatal error
	COM(0xFFFE, "Comment");

	public final int CODE;
	public final String mDescription;

	private SegmentMarker(int aCode)
	{
		this(aCode, "");
	}

	private SegmentMarker(int aCode, String aDescription)
	{
		CODE = aCode;
		mDescription = aDescription;
	}

	public static SegmentMarker valueOf(int aCode)
	{
		for (SegmentMarker sm : values())
		{
			if (sm.CODE == aCode)
			{
				return sm;
			}
		}

		return null;
	}
}


//	public static String getSOFDescription(int aMarker)
//	{
//		switch (aMarker)
//		{
//			case SOF0:
//				return "";
//			case SOF1:
//				return "Extended sequential, Huffman";
//			case SOF2:
//				return "Progressive, Huffman";
//			case SOF3:
//				return "Lossless, Huffman";
//			case SOF9:
//				return "Extended sequential, arithmetic coding";
//			case SOF10:
//				return "Progressive, arithmetic coding";
//			case SOF11:
//				return "Lossless, arithmetic coding";
//			case APP0:
//				return "JFIF image header";
//			case APP1:
//				return "Exif metadata";
//			case APP2:
//				return "FlashPix data";
//			case APP13:
//				return "XML metadata";
//			case APP14:
//				return "Adobe color profile segment";
//			case COM:
//				return "Comment";
//			case DAC:
//				return "Define Arithmetic Table";
//			case DHT:
//				return "Define Huffman Table";
//			case DQT:
//				return "Define Quantization Table";
//			case DRI:
//				return "Define Restart Interval";
//			case EOI:
//				return "End Of Image";
//			case SOI:
//				return "Start Of Image";
//			case SOS:
//				return "Start Of Scan";
//			default:
//				return "Unknown";
//		}
//	}
