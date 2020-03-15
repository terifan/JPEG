package org.terifan.imageio.jpeg.decoder;

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
import org.terifan.imageio.jpeg.FixedThreadExecutor;
import org.terifan.imageio.jpeg.ImageTransdecode;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGImage;
import org.terifan.imageio.jpeg.Log;
import org.terifan.imageio.jpeg.SegmentMarker;


public class JPEGImageReaderImpl
{
	protected FixedThreadExecutor mExecutorService;


	public JPEGImageReaderImpl()
	{
	}


	public void decode(BitInputStream aInput, JPEG aJPEG, Log aLog, IDCT aIDCT, JPEGImage aImage, boolean aDecodeCoefficients) throws IOException
	{
		int progressionLevel = 0;
		Decoder decoder = null;

		int nextSegment = aInput.readInt16();

		if (nextSegment != SegmentMarker.SOI.CODE)
		{
			throw new IOException("This isn't a JPEG image");
		}

		for (;;)
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

					aDecodeCoefficients |= sof.getCompressionType().isProgressive();

					aJPEG.mSOFSegment = sof;
					aJPEG.mCoefficients = aDecodeCoefficients ? new int[sof.getVerMCU()][sof.getHorMCU()][sof.getMaxBlocksInMCU()][64] : new int[1][sof.getHorMCU()][sof.getMaxBlocksInMCU()][64];

					decoder = compression.createDecoderInstance();
					decoder.initialize(aJPEG, aInput);

					int cn = 0;
					for (ComponentInfo comp : aJPEG.mSOFSegment.getComponents())
					{
						comp.setComponentBlockOffset(cn);
						cn += comp.getHorSampleFactor() * comp.getVerSampleFactor();
					}
					aJPEG.mBlockCount = cn;

					if (aImage != null)
					{
						aJPEG.getColorSpace().configureImageBuffer(sof, aImage);
					}
					break;
				case SOS: // Start Of Scan
					if (aImage != null && progressionLevel > 0)
					{
						aImage.endOfScan(progressionLevel);
					}

					mExecutorService = new FixedThreadExecutor(1f);

					int streamOffset = aInput.getStreamOffset();
					SOSSegment sosSegment = new SOSSegment(aJPEG).decode(aInput).print(aLog);
					sosSegment.prepareMCU();
					aInput.setHandleMarkers(true);
					readCoefficients(aJPEG, aImage, aIDCT, decoder, aDecodeCoefficients);
					aInput.setHandleMarkers(false);
					aLog.println("<image data %d bytes%s>", aInput.getStreamOffset() - streamOffset, aJPEG.mSOFSegment.getCompressionType().isProgressive() ? ", progression level " + (1 + progressionLevel) : "");
					progressionLevel++;

					mExecutorService.shutdown();

					break;
				case DRI: // Restart marker
					aInput.skipBytes(2); // skip length
					aJPEG.mRestartInterval = aInput.readInt16();
					aLog.println("%s", marker);
					aLog.println("   %d", aJPEG.mRestartInterval);
					break;
				case EOI: // End Of Image
					aLog.println("%s", marker);
					if (aImage != null)
					{
						aImage.endOfScan(0);
					}
					return;
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


	private void readCoefficients(JPEG aJPEG, JPEGImage aImage, IDCT aIDCT, Decoder aDecoder, boolean aDecodeCoefficients) throws IOException
	{
		int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
		int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();
		int numVerMCU = aJPEG.mSOFSegment.getVerMCU();
		int mcuWidth = 8 * maxSamplingX;
		int mcuHeight = 8 * maxSamplingY;

		aDecoder.startPass(aJPEG);

		int[][] mcu = new int[aJPEG.mBlockCount][64];

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
								}
							}
						}
					}
				}

				update(aJPEG, aIDCT, mcuY, aJPEG.mCoefficients[mcuY], aImage);
			}
		}
		else if (aDecodeCoefficients)
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
				}

				update(aJPEG, aIDCT, mcuY, aJPEG.mCoefficients[mcuY], aImage);
			}
		}
		else
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

								System.arraycopy(mcu[blockIndex], 0, aJPEG.mCoefficients[0][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX], 0, 64);
							}
						}
					}
				}

				update(aJPEG, aIDCT, mcuY, aJPEG.mCoefficients[0], aImage);
			}
		}

		aDecoder.finishPass(aJPEG);
	}


	public void update(JPEG aJPEG, IDCT aIDCT, int aMCUY, int[][][] aCoefficients, JPEGImage aImage)
	{
		int numHorMCU = aJPEG.mSOFSegment.getHorMCU();

		int[][][] workBlock = new int[numHorMCU][aJPEG.mBlockCount][64];

		for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
		{
			for (int blockIndex = 0; blockIndex < workBlock[0].length; blockIndex++)
			{
				System.arraycopy(aCoefficients[mcuX][blockIndex], 0, workBlock[mcuX][blockIndex], 0, 64);
			}
		}

		SOFSegment sof = aJPEG.mSOFSegment;
		int mcuW = 8 * sof.getMaxHorSampling();
		int mcuH = 8 * sof.getMaxVerSampling();

		mExecutorService.submit(()->
		{
			try
			{
				for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
				{
					int[] output = new int[mcuW * mcuH];

					ImageTransdecode.transform(aJPEG, aIDCT, aImage, workBlock[mcuX], output);

					aImage.setRGB(mcuX * mcuW, aMCUY * mcuH, mcuW, mcuH, output);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace(System.out);
			}
		});
	}
}
