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
import static org.terifan.imageio.jpeg.Debug.hexdump;
import org.terifan.imageio.jpeg.JPEG;
import static org.terifan.imageio.jpeg.JPEGConstants.*;
import org.terifan.imageio.jpeg.JPEGImage;


public class JPEGImageReaderImpl
{
	public JPEGImageReaderImpl()
	{
	}


	public JPEGImage read(BitInputStream aInput, JPEG aJPEG, IDCT aIDCT, boolean aUpdateImage, boolean aUpdateProgressiveImage) throws IOException
	{
		int progressiveLevel = 0;
		JPEGImage image = null;
		Decoder decoder = null;
		boolean stop = false;

		int nextSegment = aInput.readInt16();

		if (nextSegment != SOI) // Start Of Image
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

					hexdump(aInput);

					break;
//						throw new IOException("Error in JPEG stream; expected segment marker but found: " + Integer.toString(nextSegment, 16));
				}
			}

			if (VERBOSE)
			{
				System.out.println(Integer.toString(nextSegment, 16) + " -- " + aInput.getStreamOffset() + " (" + Integer.toHexString(aInput.getStreamOffset()) + ") -- " + getSOFDescription(nextSegment));
			}

			switch (nextSegment)
			{
				case APP0:
					new APP0Segment(aJPEG).read(aInput);
					break;
				case APP1:
					new APP1Segment(aJPEG).read(aInput);
					break;
				case APP2:
					new APP2Segment(aJPEG).read(aInput);
					break;
				case APP12:
				case APP13:
					// TODO
					aInput.skipBytes(aInput.readInt16() - 2); // skip length
					break;
				case APP14:
					new APP14Segment(aJPEG).read(aInput);
					break;
				case DQT:
					new DQTSegment(aJPEG).read(aInput);
					break;
				case DHT:
					new DHTSegment(aJPEG).read(aInput);
					break;
				case DAC: // Arithmetic Table
					new DACSegment(aJPEG).read(aInput);
					break;
				case SOF0:
//					mCompressionMode = "Baseline";
					decoder = new HuffmanDecoder(aInput);
					aJPEG.mSOFSegment = new SOFSegment(aJPEG);
					aJPEG.mSOFSegment.read(aInput);
					break;
				case SOF1:
//					mCompressionMode = "Extended sequential, Huffman";
					throw new IOException("Image encoding not supported: Extended sequential, Huffman");
				case SOF2:
//					mCompressionMode = "Progressive, Huffman";
					decoder = new HuffmanDecoder(aInput);
					aJPEG.mProgressive = true;
					aJPEG.mSOFSegment = new SOFSegment(aJPEG);
					aJPEG.mSOFSegment.read(aInput);
					break;
				case SOF9:
//					mCompressionMode = "Extended sequential, arithmetic";
					decoder = new ArithmeticDecoder(aInput);
					aJPEG.mArithmetic = true;
					aJPEG.mSOFSegment = new SOFSegment(aJPEG);
					aJPEG.mSOFSegment.read(aInput);
					break;
				case SOF10:
//					mCompressionMode = "Progressive, arithmetic";
					decoder = new ArithmeticDecoder(aInput);
					aJPEG.mArithmetic = true;
					aJPEG.mProgressive = true;
					aJPEG.mSOFSegment = new SOFSegment(aJPEG);
					aJPEG.mSOFSegment.read(aInput);
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
					SOSSegment sosSegment = new SOSSegment(aJPEG).readFrom(aInput);
					sosSegment.prepareMCU();

					try
					{
						aInput.setHandleMarkers(true);
						image = readCoefficients(aJPEG, decoder, image);
						aInput.align();
						aInput.setHandleMarkers(false);

						progressiveLevel++;

						if (VERBOSE)
						{
							System.out.println("======== " + aInput.getStreamOffset() + "(" + Integer.toHexString(aInput.getStreamOffset()) + ") / " + progressiveLevel + " ========================================================================================================================================================================");
						}

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

					break;
				case DRI:
					aInput.skipBytes(2); // skip length
					aJPEG.mRestartInterval = aInput.readInt16();
					break;
				case COM:
					aInput.skipBytes(aInput.readInt16() - 2);
					break;
				case EOI:
					if (aUpdateImage && !(aJPEG.mProgressive && aUpdateProgressiveImage))
					{
						ImageUpdater.updateImage(aJPEG, aIDCT, image.getImage());
					}
					stop = true;
					break;
				default:
					System.out.printf("Unsupported segment: %02X%n", nextSegment);

					aInput.skipBytes(aInput.readInt16() - 2);
					break;
			}
		}

		return image;
	}


	private JPEGImage readCoefficients(JPEG aJPEG, Decoder aDecoder, JPEGImage aImage) throws IOException
	{
		int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
		int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();
		int numVerMCU = aJPEG.mSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		for (int scanComponentIndex = 0, first = 0; scanComponentIndex < aJPEG.mNumComponents; scanComponentIndex++)
		{
			ComponentInfo comp = aJPEG.mSOFSegment.getComponent(scanComponentIndex);
			comp.setComponentBlockOffset(first);
			first += comp.getHorSampleFactor() * comp.getVerSampleFactor();
		}

		if (aImage == null)
		{
			aJPEG.mCoefficients = new int[numVerMCU][numHorMCU][aJPEG.mSOFSegment.getMaxBlocksInMCU()][64];

			aDecoder.initialize(aJPEG);

			aImage = new JPEGImage(aJPEG.mSOFSegment.getWidth(), aJPEG.mSOFSegment.getHeight(), maxSamplingX, maxSamplingY, aJPEG.mNumComponents);
		}

		Arrays.fill(aJPEG.entropy.last_dc_val, 0);

		if (VERBOSE)
		{
			System.out.println("  " + aDecoder.getDecoderInfo(aJPEG));
		}

		aDecoder.startPass(aJPEG);

		int[][] mcu = new int[aJPEG.blocks_in_MCU][64];

		if (aJPEG.comps_in_scan == 1)
		{
			ComponentInfo comp = aJPEG.cur_comp_info[0];

			for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
			{
				for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
				{
					if (mcuY < numVerMCU - 1 || mcuY * mcuHeight + blockY * 8 < aJPEG.mHeight)
					{
						for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
						{
							for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
							{
								if (mcuX < numHorMCU - 1 || mcuX * mcuWidth + blockX * 8 < aJPEG.mWidth)
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

		return aImage;
	}
}
