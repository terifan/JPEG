package org.terifan.imageio.jpeg.exif;

import java.util.Collection;
import java.util.HashMap;
import static org.terifan.imageio.jpeg.exif.ExifFormat.UBYTE;


public class ExifTag
{
	private final static String TABLE =
		"""
		ImageWidth, 0100, USHORT
		ImageHeight, 0101, USHORT
		ImageDescription, 010e, ASCII
		Orientation, 0112, USHORT
		Make, 010f, ASCII
		Model, 0110, ASCII
		XResolution, 011A, URATIONAL
		YResolution, 011B, URATIONAL
		ResolutionUnit, 0128, USHORT
		Software, 0131, ASCII
		DateTime, 0132, UBYTE
		Artist, 013b, ASCII
		WhitePoint, 013e, URATIONAL
		PrimaryChromaticities, 013f, URATIONAL
		YCbCrCoefficients, 0211, URATIONAL
		YCbCrPositioning, 0213, USHORT
		ReferenceBlackWhite, 0214, URATIONAL
		Copyright, 8298, ASCII
		ExifOffset, 8769, ULONG
		GpsOffset, 8825, ULONG
		GPSDOP, 000B, RATIONAL

		ExposureTime, 829a, URATIONAL
		FNumber, 829d, URATIONAL
		ExposureProgram, 8822, USHORT
		ISOSpeedRatings, 8827, USHORT
		ExifVersion, 9000, ULONG
		DateTimeOriginal, 9003, UBYTE
		DateTimeDigitized, 9004, UBYTE
		ComponentConfiguration, 9101, UNDEFINED
		CompressedBitsPerPixel, 9102, URATIONAL
		ShutterSpeedValue, 9201, RATIONAL
		ApertureValue, 9202, URATIONAL
		BrightnessValue, 9203, RATIONAL
		ExposureBiasValue, 9204, RATIONAL
		MaxApertureValue, 9205, URATIONAL
		SubjectDistance, 9206, RATIONAL
		MeteringMode, 9207, USHORT
		LightSource, 9208, USHORT
		Flash, 9209, USHORT
		FocalLength, 920a, URATIONAL
		MakerNote, 927c, UNDEFINED
		UserComment, 9286, UNDEFINED
		FlashPixVersion, a000, UNDEFINED
		ColorSpace, a001, USHORT
		ExifImageWidth, a002, USHORT
		ExifImageHeight, a003, USHORT
		RelatedSoundFile, a004, ASCII
		ExifInteroperabilityOffset, a005, ULONG
		FocalPlaneXResolution, a20e, URATIONAL
		FocalPlaneYResolution, a20f, URATIONAL
		FocalPlaneResolutionUnit, a210, USHORT
		SensingMethod, a217, USHORT
		FileSource, a300, UNDEFINED
		SceneType, a301, UNDEFINED

		ThumbWidth, 0100, USHORT
		ThumbHeight, 0101, USHORT
		ThumbBitsPerSample, 0102, USHORT
		ThumbCompression, 0103, USHORT
		ThumbPhotometricInterpretation, 0106, USHORT
		ThumbStripOffsets, 0111, USHORT
		ThumbSamplesPerPixel, 0115, USHORT
		ThumbRowsPerStrip, 0116, USHORT
		ThumbStripByteConunts, 0117, USHORT
		ThumbXResolution, 011a, URATIONAL
		ThumbYResolution, 011b, URATIONAL
		ThumbPlanarConfiguration, 011c, USHORT
		ThumbResolutionUnit, 0128, USHORT
		ThumbJpegIFOffset, 0201, ULONG
		ThumbJpegIFByteCount, 0202, ULONG
		ThumbYCbCrCoefficients, 0211, URATIONAL
		ThumbYCbCrSubSampling, 0212, USHORT
		ThumbYCbCrPositioning, 0213, USHORT
		ThumbReferenceBlackWhite, 0214, URATIONAL

		RatingNumber, 4746, USHORT
		RatingPercent, 4749, USHORT
		ImageNumber, 9211, ULONG
		_Title, 9C9B, UBYTE
		ImageUniqueID, A420, ASCII
		SubSecTime, 9290, ASCII
		SubSecTimeOriginal, 9291, ASCII
		SubSecTimeDigitized, 9292, ASCII

		Comment, 9C9c, UBYTE
		Author, 9C9d, UBYTE
		Tags, 9c9e, UBYTE
		Subject, 9c9f, UBYTE

		CustomImageProcessing, A401, USHORT
		ExposureMode, A402, USHORT
		WhiteBalance, A403, USHORT
		DigitalZoomRatio, A404, RATIONAL
		FocalLengthIn35mmFilm, A405, USHORT
		SceneCaptureType, A406, USHORT
		GainControl, A407, RATIONAL
		Contrast, A408, USHORT
		Saturation, A409, USHORT
		Sharpness, A40A, USHORT
		DeviceSettingDescription, A40B, UNDEFINED
		SubjectDistanceRange, A40C0, USHORT""";

	public final static ExifTag PADDING = new ExifTag("Padding", 0xea1c, UBYTE);

	private final static HashMap<Integer, ExifTag> mTags = new HashMap<>();


	public final String name;
	public final int code;
	public final ExifFormat format;


	private ExifTag(String aName, int aCode, ExifFormat aFormat)
	{
		name = aName;
		code = aCode;
		format = aFormat;
	}


	public Collection<ExifTag> values()
	{
		install();
		return mTags.values();
	}


	public static ExifTag valueOf(String aName)
	{
		if ("padding".equalsIgnoreCase(aName))
		{
			return PADDING;
		}
		install();
		for (ExifTag tag : mTags.values())
		{
			if (tag.name.equalsIgnoreCase(aName))
			{
				return tag;
			}
		}

		return null;
	}


	public static ExifTag valueOf(int aCode, ExifFormat aFormat)
	{
		if (aCode == PADDING.code && PADDING.format == aFormat)
		{
			return PADDING;
		}
		if (mTags.isEmpty())
		{
			install();
		}
		ExifTag tmp = mTags.get(aCode);
		if (tmp != null && tmp.format == aFormat)
		{
			return tmp;
		}
		if (tmp != null)
		{
			return new ExifTag(tmp.name, aCode, aFormat);
		}

		return new ExifTag("Unknown" + aCode, aCode, aFormat);
	}


	@Override
	public int hashCode()
	{
		return Integer.hashCode(code);
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (obj instanceof ExifTag other)
		{
			return this.code == other.code;
		}
		return false;
	}


	@Override
	public String toString()
	{
		return name;
	}


	private static synchronized void install()
	{
		if (mTags.isEmpty())
		{
			for (String s : TABLE.split("\n"))
			{
				if (!s.isBlank())
				{
					String[] fields = s.split(",");
					String name = fields[0].trim();
					int code = Integer.parseInt(fields[1].trim(), 16);
					ExifFormat format = ExifFormat.valueOf(fields[2].trim());
					mTags.put(code, new ExifTag(name, code, format));
				}
			}
		}
	}
}