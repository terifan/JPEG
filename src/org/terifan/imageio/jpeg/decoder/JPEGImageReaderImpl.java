package org.terifan.imageio.jpeg.decoder;

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
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.JPEGImage;
import org.terifan.imageio.jpeg.Log;
import org.terifan.imageio.jpeg.SegmentMarker;
import org.terifan.imageio.jpeg.test.Debug;


public class JPEGImageReaderImpl
{
	public JPEGImageReaderImpl()
	{
	}


	public JPEGImage decode(BitInputStream aInput, JPEG aJPEG, Log aLog, IDCT aIDCT, boolean aUpdateImage, boolean aUpdateProgressiveImage) throws IOException
	{
		int progressionLevel = 0;
		JPEGImage image = null;
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
					if (aUpdateImage)
					{
						ImageUpdater.updateImage(aJPEG, aIDCT, image.getImage());
					}

					System.out.println("Expected JPEG marker at " + aInput.getStreamOffset() + " (" + Integer.toHexString(aInput.getStreamOffset()) + ")");

					Debug.hexDump(aInput);

					break;
//						throw new IOException("Error in JPEG stream; expected segment marker but found: " + Integer.toString(nextSegment, 16));
				}
			}

			SegmentMarker marker = SegmentMarker.valueOf(nextSegment);

			if (marker == null)
			{
				int length = aInput.readInt16() - 2;
				aInput.skipBytes(length);

				aLog.println("<unsupported segment " + Integer.toHexString(nextSegment) + ", " + length + " bytes>");
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
				case SOF0:
//					mCompressionMode = "Baseline";
					decoder = new HuffmanDecoder(aInput);
					aJPEG.mSOFSegment = new SOFSegment(aJPEG);
					aJPEG.mSOFSegment.decode(aInput).print(aLog);
					break;
				case SOF1:
//					mCompressionMode = "Extended sequential, Huffman";
					throw new IOException("Image encoding not supported: Extended sequential, Huffman");
				case SOF2:
//					mCompressionMode = "Progressive, Huffman";
					decoder = new HuffmanDecoder(aInput);
					aJPEG.mProgressive = true;
					aJPEG.mSOFSegment = new SOFSegment(aJPEG);
					aJPEG.mSOFSegment.decode(aInput).print(aLog);
					break;
				case SOF9:
//					mCompressionMode = "Extended sequential, arithmetic";
					decoder = new ArithmeticDecoder(aInput);
					aJPEG.mArithmetic = true;
					aJPEG.mSOFSegment = new SOFSegment(aJPEG);
					aJPEG.mSOFSegment.decode(aInput).print(aLog);
					break;
				case SOF10:
//					mCompressionMode = "Progressive, arithmetic";
					decoder = new ArithmeticDecoder(aInput);
					aJPEG.mArithmetic = true;
					aJPEG.mProgressive = true;
					aJPEG.mSOFSegment = new SOFSegment(aJPEG);
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
						image = new JPEGImage(sof.getWidth(), sof.getHeight(), sof.getMaxHorSampling(), sof.getMaxVerSampling(), sof.getComponents().length);
						aJPEG.mCoefficients = new int[sof.getVerMCU()][sof.getHorMCU()][sof.getMaxBlocksInMCU()][64];
					}

					progressionLevel++;
					int streamOffset = aInput.getStreamOffset();
					SOSSegment sosSegment = new SOSSegment(aJPEG);
					sosSegment.decode(aInput).print(aLog);
					sosSegment.prepareMCU();

					try
					{
						aInput.setHandleMarkers(true);

						readCoefficients(aJPEG, decoder, progressionLevel);

						aInput.align();
						aInput.setHandleMarkers(false);

						if (aUpdateImage && aJPEG.mProgressive && aUpdateProgressiveImage)
						{
							ImageUpdater.updateImage(aJPEG, aIDCT, image.getImage());
						}
					}
					catch (Throwable e)
					{
						e.printStackTrace(System.out);
						stop = true;
					}

					aLog.println("<image data " + (aInput.getStreamOffset() - streamOffset) + " bytes" + (aJPEG.mProgressive ? ", progression level " + progressionLevel : "") + ">");

					break;
				case DRI:
					aInput.skipBytes(2); // skip length
					aJPEG.mRestartInterval = aInput.readInt16();
					aLog.println(marker);
					aLog.println("   " + aJPEG.mRestartInterval);
					break;
				case EOI:
					aLog.println(SegmentMarker.EOI);
					if (aUpdateImage && !(aJPEG.mProgressive && aUpdateProgressiveImage))
					{
						ImageUpdater.updateImage(aJPEG, aIDCT, image.getImage());
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
					aLog.println("<unsupported segment " + marker + ", " + length + " bytes>");
					break;
			}
		}

		return image;
	}


	private void readCoefficients(JPEG aJPEG, Decoder aDecoder, int aProgressionLevel) throws IOException
	{
		int numComponents = aJPEG.mSOFSegment.getComponents().length;
		int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
		int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();
		int numVerMCU = aJPEG.mSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		for (int scanComponentIndex = 0, first = 0; scanComponentIndex < numComponents; scanComponentIndex++)
		{
			ComponentInfo comp = aJPEG.mSOFSegment.getComponent(scanComponentIndex);
			comp.setComponentBlockOffset(first);
			first += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		if (aProgressionLevel == 1)
		{
			aDecoder.initialize(aJPEG);
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
	}
}
