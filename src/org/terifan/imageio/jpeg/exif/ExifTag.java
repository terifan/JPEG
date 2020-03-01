package org.terifan.imageio.jpeg.exif;

import static org.terifan.imageio.jpeg.exif.ExifFormat.RATIONAL;
import static org.terifan.imageio.jpeg.exif.ExifFormat.UBYTE;
import static org.terifan.imageio.jpeg.exif.ExifFormat.ULONG;
import static org.terifan.imageio.jpeg.exif.ExifFormat.UNDEFINED;
import static org.terifan.imageio.jpeg.exif.ExifFormat.URATIONAL;
import static org.terifan.imageio.jpeg.exif.ExifFormat.USHORT;
import static org.terifan.imageio.jpeg.exif.ExifFormat.ASCII;


public enum ExifTag
{
	// Tags used by IFD0 (main image)
	ImageWidth(0x0100, USHORT),
	ImageHeight(0x0101, USHORT),
	ImageDescription(0x010e, ASCII),
	Orientation(0x0112, USHORT),
	Make(0x010f, ASCII),
	Model(0x0110, ASCII),
	XResolution(0x011A, URATIONAL),
	YResolution(0x011B, URATIONAL),
	ResolutionUnit(0x0128, USHORT),
	Software(0x0131, ASCII),
	DateTime(0x0132, UBYTE),
	WhitePoint(0x013e, URATIONAL),
	PrimaryChromaticities(0x013f, URATIONAL),
	YCbCrCoefficients(0x0211, URATIONAL),
	YCbCrPositioning(0x0213, USHORT),
	ReferenceBlackWhite(0x0214, URATIONAL),
	Copyright(0x8298, ASCII),
	ExifOffset(0x8769, ULONG),
	GpsOffset(0x8825, ULONG),
	GPSDOP(0x000B, RATIONAL),

	// Tags used by Exif SubIFD
	ExposureTime(0x829a, URATIONAL),
	FNumber(0x829d, URATIONAL),
	ExposureProgram(0x8822, USHORT),
	ISOSpeedRatings(0x8827, USHORT),
	ExifVersion(0x9000, ULONG),
	DateTimeOriginal(0x9003, UBYTE),
	DateTimeDigitized(0x9004, UBYTE),
	ComponentConfiguration(0x9101, UNDEFINED),
	CompressedBitsPerPixel(0x9102, URATIONAL),
	ShutterSpeedValue(0x9201, RATIONAL),
	ApertureValue(0x9202, URATIONAL),
	BrightnessValue(0x9203, RATIONAL),
	ExposureBiasValue(0x9204, RATIONAL),
	MaxApertureValue(0x9205, URATIONAL),
	SubjectDistance(0x9206, RATIONAL),
	MeteringMode(0x9207, USHORT),
	LightSource(0x9208, USHORT),
	Flash(0x9209, USHORT),
	FocalLength(0x920a, URATIONAL),
	MakerNote(0x927c, UNDEFINED),
	UserComment(0x9286, UNDEFINED),
	FlashPixVersion(0xa000, UNDEFINED),
	ColorSpace(0xa001, USHORT),
	ExifImageWidth(0xa002, USHORT),
	ExifImageHeight(0xa003, USHORT),
	RelatedSoundFile(0xa004, ASCII),
	ExifInteroperabilityOffset(0xa005, ULONG),
	FocalPlaneXResolution(0xa20e, URATIONAL),
	FocalPlaneYResolution(0xa20f, URATIONAL),
	FocalPlaneResolutionUnit(0xa210, USHORT),
	SensingMethod(0xa217, USHORT),
	FileSource(0xa300, UNDEFINED),
	SceneType(0xa301, UNDEFINED),

	// Tags used by IFD1 (thumbnail image)
	ThumbWidth(0x0100, USHORT),
	ThumbHeight(0x0101, USHORT),
	ThumbBitsPerSample(0x0102, USHORT),
	ThumbCompression(0x0103, USHORT),
	ThumbPhotometricInterpretation(0x0106, USHORT),
	ThumbStripOffsets(0x0111, USHORT),
	ThumbSamplesPerPixel(0x0115, USHORT),
	ThumbRowsPerStrip(0x0116, USHORT),
	ThumbStripByteConunts(0x0117, USHORT),
	ThumbXResolution(0x011a, URATIONAL),
	ThumbYResolution(0x011b, URATIONAL),
	ThumbPlanarConfiguration(0x011c, USHORT),
	ThumbResolutionUnit(0x0128, USHORT),
	ThumbJpegIFOffset(0x0201, ULONG),
	ThumbJpegIFByteCount(0x0202, ULONG),
	ThumbYCbCrCoefficients(0x0211, URATIONAL),
	ThumbYCbCrSubSampling(0x0212, USHORT),
	ThumbYCbCrPositioning(0x0213, USHORT),
	ThumbReferenceBlackWhite(0x0214, URATIONAL),

	RatingNumber(0x4746, USHORT),
	RatingPercent(0x4749, USHORT),
	ImageNumber(0x9211, ULONG),
	_Title(0x9C9B, UBYTE),
	ImageUniqueID(0xA420, ASCII),
	SubSecTime(0x9290, ASCII),
	SubSecTimeOriginal(0x9291, ASCII),
	SubSecTimeDigitized(0x9292, ASCII),

	Comment(0x9C9c, UBYTE),
	Author(0x9C9d, UBYTE),
	Tags(0x9c9e, UBYTE),
	Subject(0x9c9f, UBYTE),

	CustomImageProcessing(0xA401, USHORT),
	ExposureMode(0xA402, USHORT),
	WhiteBalance(0xA403, USHORT),
	DigitalZoomRatio(0xA404, RATIONAL),
	FocalLengthIn35mmFilm(0xA405, USHORT),
	SceneCaptureType(0xA406, USHORT),
	GainControl(0xA407, RATIONAL),
	Contrast(0xA408, USHORT),
	Saturation(0xA409, USHORT),
	Sharpness(0xA40A, USHORT),
	DeviceSettingDescription(0xA40B, UNDEFINED),
	SubjectDistanceRange(0xA40C0, USHORT),

	PADDING(0xea1c, UBYTE)
	;

	final int CODE;
	final ExifFormat mFormat;


	private ExifTag(int aCode, ExifFormat aFormat)
	{
		CODE = aCode;
		mFormat = aFormat;
	}


	static ExifTag decode(int aCode)
	{
		for (ExifTag f : values())
		{
			if (f.CODE == aCode)
			{
				return f;
			}
		}
		return null;
	}


	public static ExifTag lookup(int aCode)
	{
		for (ExifTag type : values())
		{
			if (type.CODE == aCode)
			{
				return type;
			}
		}
		return null;
	}
}