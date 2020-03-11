package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.ImageTransdecode;
import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.SOSSegment;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DQTSegment;
import java.io.IOException;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP14Segment;
import org.terifan.imageio.jpeg.APP1Segment;
import org.terifan.imageio.jpeg.APP2Segment;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.Log;
import org.terifan.imageio.jpeg.SegmentMarker;


public class JPEGImageReaderImpl
{
	private boolean mUpdateImage;
	public JPEGImageReaderImpl()
	{
	}


	public BufferedImage decode(BitInputStream aInput, JPEG aJPEG, Log aLog, IDCT aIDCT, boolean aUpdateImage, boolean aUpdateProgressiveImage, boolean aDecodeCoefficients) throws IOException
	{
		BufferedImage image = null;

		mUpdateImage = aUpdateImage;

		try
		{
			int progressionLevel = 0;
			Decoder decoder = null;

			int nextSegment = aInput.readInt16();

			if (nextSegment != SegmentMarker.SOI.CODE) // Start Of Image
			{
				throw new IOException("Error in JPEG stream; expected SOI segment marker but found: " + Integer.toString(nextSegment, 16));
			}

			for (boolean stop = false; !stop;)
			{
				if (aInput.getUnreadMarker() != 0)
				{
					nextSegment = 0xff00 | aInput.getUnreadMarker();
					aInput.setUnreadMarker(0);
				}
				else
				{
					nextSegment = aInput.readInt16();

					if ((nextSegment & 0xFF00) != 0xFF00 || nextSegment == -1)
					{
//						updateImage(aUpdateImage, aJPEG, aUpdateProgressiveImage, aIDCT, image);

						throw new IOException("Error in JPEG stream at offset " + aInput.getStreamOffset() + "; expected segment marker but found: " + Integer.toString(nextSegment, 16));
					}
				}

				SegmentMarker marker = SegmentMarker.valueOf(nextSegment);

				if (marker == null)
				{
					int length = aInput.readInt16() - 2;
					aInput.skipBytes(length);

					aLog.println("<unsupported segment %s, %d bytes>", Integer.toHexString(nextSegment), length);
					continue;
				}

				switch (marker)
				{
					case APP0: // Thumbnail
						new APP0Segment(aJPEG).decode(aInput).print(aLog);
						break;
					case APP1: // Exif
						new APP1Segment(aJPEG).decode(aInput).print(aLog);
						break;
					case APP2: // Color space
						new APP2Segment(aJPEG).decode(aInput).print(aLog);
						break;
					case APP14: // Adobe color profiles
						new APP14Segment(aJPEG).decode(aInput).print(aLog);
						break;
					case DQT: // Quantisations tables
						new DQTSegment(aJPEG).decode(aInput).print(aLog);
						break;
					case DHT: // Huffman Table
						new DHTSegment(aJPEG).decode(aInput).print(aLog);
						break;
					case DAC: // Arithmetic Table
						new DACSegment(aJPEG).decode(aInput).print(aLog);
						break;
					case SOF0: // Huffman
					case SOF1: // Huffman extended
					case SOF2: // Huffman progressive
					case SOF3: // Lossless, Huffman
					case SOF5: // Differential sequential, Huffman
					case SOF6: // Differential progressive, Huffman
					case SOF7: // Differential lossless, Huffman
					case SOF9: // Arithmetic
					case SOF10: // Arithmetic progressive
					case SOF11: // Lossless, arithmetic
					case SOF13: // Differential sequential, arithmetic
					case SOF14: // Differential progressive, arithmetic
					case SOF15: // Differential lossless, arithmetic
						CompressionType compression = CompressionType.decode(marker);

						SOFSegment sof = new SOFSegment(aJPEG).decode(aInput).setCompressionType(compression).print(aLog);

						aJPEG.mSOFSegment = sof;
						aJPEG.mCoefficients = aJPEG.mSOFSegment.getCompressionType().isProgressive() || aDecodeCoefficients ? new int[sof.getVerMCU()][sof.getHorMCU()][sof.getMaxBlocksInMCU()][64] : new int[sof.getVerMCU()][sof.getHorMCU()][sof.getMaxBlocksInMCU()][64];

						decoder = compression.createDecoderInstance();
						decoder.initialize(aJPEG, aInput);

						image = new BufferedImage(sof.getWidth(), sof.getHeight(), sof.getComponents().length == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_RGB);
						break;
					case SOS: // Start Of Scan
						int streamOffset = aInput.getStreamOffset();
						SOSSegment sosSegment = new SOSSegment(aJPEG).decode(aInput).print(aLog);
						sosSegment.prepareMCU();
						aInput.setHandleMarkers(true);
						readCoefficients(aJPEG, decoder, progressionLevel, aDecodeCoefficients, aIDCT, image);
						aInput.setHandleMarkers(false);
//						updateImage(aUpdateImage, aJPEG, aUpdateProgressiveImage, aIDCT, image);
						aLog.println("<image data %d bytes%s>", aInput.getStreamOffset() - streamOffset, aJPEG.mSOFSegment.getCompressionType().isProgressive() ? ", progression level " + (1 + progressionLevel) : "");
						progressionLevel++;
						break;
					case DRI: // Restart marker
						aInput.skipBytes(2); // skip length
						aJPEG.mRestartInterval = aInput.readInt16();
						aLog.println(marker.toString());
						aLog.println("   " + aJPEG.mRestartInterval);
						break;
					case EOI: // End Of Image
						aLog.println(SegmentMarker.EOI.name());
//						updateImage(aUpdateImage, aJPEG, aUpdateProgressiveImage, aIDCT, image);
						stop = true;
						break;
					case APP12:
					case APP13:
					case COM: // Comment
						// TODO
					default:
						int length = aInput.readInt16() - 2;
						aInput.skipBytes(length);
						aLog.println("<unsupported segment %s, %d bytes>", marker, length);
						break;
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}

		return image;
	}


	private void readCoefficients(JPEG aJPEG, Decoder aDecoder, int aProgressionLevel, boolean aDecodeCoefficients, IDCT aIDCT, BufferedImage aImage) throws IOException
	{
		int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
		int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();
		int numVerMCU = aJPEG.mSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		int cn = 0;
		for (ComponentInfo comp : aJPEG.mSOFSegment.getComponents())
		{
			comp.setComponentBlockOffset(cn);
			cn += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		aDecoder.startPass(aJPEG);

		int[][] mcu = new int[aJPEG.mMCUBlockCount][64];

		if (aJPEG.mScanBlockCount == 1)
		{
			ComponentInfo comp = aJPEG.mComponentInfo[0];

			for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
			{
				for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
				{
					if (mcuY < numVerMCU - 1 || mcuY * mcuHeight + blockY * 8 < aJPEG.mSOFSegment.getHeight())
					{
						for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
						{
							for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
							{
								if (mcuX < numHorMCU - 1 || mcuX * mcuWidth + blockX * 8 < aJPEG.mSOFSegment.getWidth())
								{
									mcu[0] = aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

									aDecoder.decodeMCU(aJPEG, mcu);

									updateMCU(mcuX, mcuY, aJPEG, aIDCT, aImage);
								}
							}
						}
					}
				}
			}
		}
		else if (aJPEG.mSOFSegment.getCompressionType().isProgressive() || aDecodeCoefficients)
		{
			for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
			{
				for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
				{
					for (int ci = 0, blockIndex = 0; ci < aJPEG.mScanBlockCount; ci++)
					{
						ComponentInfo comp = aJPEG.mComponentInfo[ci];

						for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
						{
							for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++, blockIndex++)
							{
								mcu[blockIndex] = aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];
							}
						}
					}

					aDecoder.decodeMCU(aJPEG, mcu);

					updateMCU(mcuX, mcuY, aJPEG, aIDCT, aImage);
				}
			}
		}
		else // decoding one mcu at a time
		{
			for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
			{
				for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
				{
					aDecoder.decodeMCU(aJPEG, mcu);

					for (int ci = 0, blockIndex = 0; ci < aJPEG.mScanBlockCount; ci++)
					{
						ComponentInfo comp = aJPEG.mComponentInfo[ci];

						for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
						{
							for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++, blockIndex++)
							{
//								aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX] = mcu[blockIndex];

								System.arraycopy(mcu[blockIndex], 0, aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX], 0, 64);
							}
						}
					}

					updateMCU(mcuX, mcuY, aJPEG, aIDCT, aImage);
				}
			}
		}

		aDecoder.finishPass(aJPEG);
	}


	private void updateMCU(int aMCUX, int aMCUY, JPEG aJPEG, IDCT aIDCT, BufferedImage aImage)
	{
		if (mUpdateImage)
		ImageTransdecode.transform(aJPEG, aIDCT, aImage, aMCUX, aMCUY);
	}


//	private void updateImage(boolean aUpdateImage, JPEG aJPEG, boolean aUpdateProgressiveImage, IDCT aIDCT, BufferedImage aImage)
//	{
//		if (aUpdateImage && aImage != null)
//		{
//			ImageTransdecode.transform(aJPEG, aIDCT, aImage);
//		}
//
//		if (aUpdateImage && aJPEG.mSOFSegment.getCompressionType().isProgressive() && aUpdateProgressiveImage)
//		{
//			ImageTransdecode.transform(aJPEG, aIDCT, aImage);
//		}
//
//		if (aUpdateImage && !(aJPEG.mSOFSegment.getCompressionType().isProgressive() && aUpdateProgressiveImage))
//		{
//			ImageTransdecode.transform(aJPEG, aIDCT, aImage);
//		}
//	}
}
