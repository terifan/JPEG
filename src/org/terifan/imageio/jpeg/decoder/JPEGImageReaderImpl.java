package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.ImageTransdecode;
import java.awt.image.BufferedImage;
import org.terifan.imageio.jpeg.SOSSegment;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DQTSegment;
import java.io.IOException;
import java.util.Arrays;
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
	public JPEGImageReaderImpl()
	{
	}


	public BufferedImage decode(BitInputStream aInput, JPEG aJPEG, Log aLog, IDCT aIDCT, boolean aUpdateImage, boolean aUpdateProgressiveImage) throws IOException
	{
		int progressionLevel = 0;
		BufferedImage image = null;
		Decoder decoder = null;
		boolean stop = false;

		int nextSegment = aInput.readInt16();

		if (nextSegment != SegmentMarker.SOI.CODE) // Start Of Image
		{
			throw new IOException("Error in JPEG stream; expected SOI segment marker but found: " + Integer.toString(nextSegment, 16));
		}

		while (!stop)
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
					if (aUpdateImage && image != null)
					{
						ImageTransdecode.transform(aJPEG, aIDCT, image);
					}

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
				case APP0:
					new APP0Segment(aJPEG).decode(aInput).print(aLog);
					break;
				case APP1:
					new APP1Segment(aJPEG).decode(aInput).print(aLog);
					break;
				case APP2:
					new APP2Segment(aJPEG).decode(aInput).print(aLog);
					break;
				case APP14:
					new APP14Segment(aJPEG).decode(aInput).print(aLog);
					break;
				case DQT:
					new DQTSegment(aJPEG).decode(aInput).print(aLog);
					break;
				case DHT:
					new DHTSegment(aJPEG).decode(aInput).print(aLog);
					break;
				case DAC: // Arithmetic Table
					new DACSegment(aJPEG).decode(aInput).print(aLog);
					break;
				case SOF1:
					throw new IOException("Image encoding not supported: Extended sequential, Huffman");
				case SOF0:
					decoder = new HuffmanDecoder(aInput);
					aJPEG.mSOFSegment = new SOFSegment(aJPEG, CompressionType.Huffman);
					aJPEG.mSOFSegment.decode(aInput).print(aLog);
					break;
				case SOF2:
					decoder = new HuffmanDecoder(aInput);
					aJPEG.mSOFSegment = new SOFSegment(aJPEG, CompressionType.HuffmanProgressive);
					aJPEG.mSOFSegment.decode(aInput).print(aLog);
					break;
				case SOF9:
					decoder = new ArithmeticDecoder(aInput);
					aJPEG.mSOFSegment = new SOFSegment(aJPEG, CompressionType.Arithmetic);
					aJPEG.mSOFSegment.decode(aInput).print(aLog);
					break;
				case SOF10:
					decoder = new ArithmeticDecoder(aInput);
					aJPEG.mSOFSegment = new SOFSegment(aJPEG, CompressionType.ArithmeticProgressive);
					aJPEG.mSOFSegment.decode(aInput).print(aLog);
					break;
				case SOF3: // Lossless, Huffman
				case SOF5: // Differential sequential, Huffman
				case SOF6: // Differential progressive, Huffman
				case SOF7: // Differential lossless, Huffman
				case SOF11: // Lossless, arithmetic
				case SOF13: // Differential sequential, arithmetic
				case SOF14: // Differential progressive, arithmetic
				case SOF15: // Differential lossless, arithmetic
					throw new IOException("Image encoding not supported.");
				case SOS:
					if (image == null)
					{
						SOFSegment sof = aJPEG.mSOFSegment;
						image = new BufferedImage(sof.getWidth(), sof.getHeight(), sof.getComponents().length == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_RGB);
						aJPEG.mCoefficients = new int[sof.getVerMCU()][sof.getHorMCU()][sof.getMaxBlocksInMCU()][64];
					}

					int streamOffset = aInput.getStreamOffset();
					SOSSegment sosSegment = new SOSSegment(aJPEG);
					sosSegment.decode(aInput).print(aLog);
					sosSegment.prepareMCU();

					try
					{
						aInput.setHandleMarkers(true);

						readCoefficients(aJPEG, decoder, progressionLevel);

						aInput.setHandleMarkers(false);

						if (aUpdateImage && aJPEG.mSOFSegment.getCompressionType().isProgressive() && aUpdateProgressiveImage)
						{
							ImageTransdecode.transform(aJPEG, aIDCT, image);
						}
					}
					catch (Throwable e)
					{
						e.printStackTrace(System.out);
						stop = true;
					}

					aLog.println("<image data %d bytes%s>", aInput.getStreamOffset() - streamOffset, aJPEG.mSOFSegment.getCompressionType().isProgressive() ? ", progression level " + (1 + progressionLevel) : "");

					progressionLevel++;

					break;
				case DRI:
					aInput.skipBytes(2); // skip length
					aJPEG.mRestartInterval = aInput.readInt16();
					aLog.println(marker.toString());
					aLog.println("   " + aJPEG.mRestartInterval);
					break;
				case EOI:
					aLog.println(SegmentMarker.EOI.name());
					if (aUpdateImage && !(aJPEG.mSOFSegment.getCompressionType().isProgressive() && aUpdateProgressiveImage))
					{
						ImageTransdecode.transform(aJPEG, aIDCT, image);
					}
					stop = true;
					break;
				case APP12:
				case APP13:
				case COM:
					// TODO
				default:
					int length = aInput.readInt16() - 2;
					aInput.skipBytes(length);
					aLog.println("<unsupported segment %s, %d bytes>", marker, length);
					break;
			}
		}

		return image;
	}


	private void readCoefficients(JPEG aJPEG, Decoder aDecoder, int aProgressionLevel) throws IOException
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

		if (aProgressionLevel == 0)
		{
			aDecoder.initialize(aJPEG, aJPEG.mSOFSegment.getCompressionType().isProgressive());
		}

		Arrays.fill(aJPEG.entropy.last_dc_val, 0);

//		System.out.println("  " + aDecoder.getDecoderInfo(aJPEG));

		aDecoder.startPass(aJPEG);

		int[][] mcu = new int[aJPEG.blocks_in_MCU][64];

		if (aJPEG.comps_in_scan == 1)
		{
			ComponentInfo comp = aJPEG.cur_comp_info[0];

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
								}
							}
						}
					}
				}
			}
		}
		else
		{
			for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
			{
				for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
				{
					for (int ci = 0, blockIndex = 0; ci < aJPEG.comps_in_scan; ci++)
					{
						ComponentInfo comp = aJPEG.cur_comp_info[ci];

						for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
						{
							for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++, blockIndex++)
							{
								mcu[blockIndex] = aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];
							}
						}
					}

					aDecoder.decodeMCU(aJPEG, mcu);

					for (int ci = 0, blockIndex = 0; ci < aJPEG.comps_in_scan; ci++)
					{
						ComponentInfo comp = aJPEG.cur_comp_info[ci];

						for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
						{
							for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++, blockIndex++)
							{
								aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX] = mcu[blockIndex];
							}
						}
					}
				}
			}
		}

		aDecoder.finishPass(aJPEG);
	}
}
