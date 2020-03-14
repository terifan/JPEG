package org.terifan.imageio.jpeg.encoder;

import java.io.IOException;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP2Segment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.CompressionType;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.DQTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.Log;
import org.terifan.imageio.jpeg.SOSSegment;
import org.terifan.imageio.jpeg.SegmentMarker;


public class JPEGImageWriterImpl
{
	public void create(JPEG aJPEG, BitOutputStream aOutput, Log aLog) throws IOException
	{
		aOutput.writeInt16(SegmentMarker.SOI.CODE);

		new APP0Segment(aJPEG).encode(aOutput).log(aLog);

		if (aJPEG.mICCProfile != null)
		{
			new APP2Segment(aJPEG).setType(APP2Segment.ICC_PROFILE).encode(aOutput);
		}

		new DQTSegment(aJPEG).encode(aOutput).log(aLog);

		aJPEG.mSOFSegment.encode(aOutput).log(aLog);
	}


	public void finish(JPEG aJPEG, BitOutputStream aOutput, Log aLog) throws IOException
	{
		aOutput.writeInt16(SegmentMarker.EOI.CODE);
		aLog.println("EOI");
	}


	public void encode(JPEG aJPEG, BitOutputStream aOutput, Log aLog, CompressionType aCompressionType, ProgressionScript aProgressionScript) throws IOException
	{
		if (aCompressionType.isProgressive() && aProgressionScript == null)
		{
			aProgressionScript = ProgressionScript.DEFAULT;
		}

		int num_hor_mcu = aJPEG.mSOFSegment.getHorMCU();
		int num_ver_mcu = aJPEG.mSOFSegment.getVerMCU();

		Encoder encoder = null;

		int progressionLevels = aCompressionType.isProgressive() ? aProgressionScript.getParams().size() : 1;

		for (int progressionLevel = 0; progressionLevel < progressionLevels; progressionLevel++)
		{
			SOSSegment sosSegment;

			if (aCompressionType.isProgressive())
			{
				int[][] params = aProgressionScript.getParams().get(progressionLevel);

				aJPEG.Ss = params[1][0];
				aJPEG.Se = params[1][1];
				aJPEG.Ah = params[1][2];
				aJPEG.Al = params[1][3];

				int[] id = new int[params[0].length];
				for (int i = 0; i < params[0].length; i++)
				{
					id[i] = aJPEG.mSOFSegment.getComponents()[params[0][i]].getComponentId();
				}

				sosSegment = new SOSSegment(aJPEG, id);

				if (params[0].length == 3)
				{
					if (progressionLevel == 0)
					{
						sosSegment.setTableDC(0, 0);
						sosSegment.setTableAC(0, 0);
						sosSegment.setTableDC(1, 1);
						sosSegment.setTableAC(1, 0);
						sosSegment.setTableDC(2, 1);
						sosSegment.setTableAC(2, 0);
					}
					else
					{
						sosSegment.setTableDC(0, 0);
						sosSegment.setTableAC(0, 0);
						sosSegment.setTableDC(1, 0);
						sosSegment.setTableAC(1, 0);
						sosSegment.setTableDC(2, 0);
						sosSegment.setTableAC(2, 0);
					}
				}
				else
				{
					sosSegment.setTableDC(0, 0);
					sosSegment.setTableAC(0, id[0] == 1 ? 0 : 1);
				}

				int cn = 0;
				for (ComponentInfo comp : aJPEG.mSOFSegment.getComponents())
				{
					comp.setComponentBlockOffset(cn);
					cn += comp.getHorSampleFactor() * comp.getVerSampleFactor();
				}
			}
			else
			{
				aJPEG.Ss = 0;
				aJPEG.Se = 63;
				aJPEG.Ah = 0;
				aJPEG.Al = 0;

				sosSegment = new SOSSegment(aJPEG, aJPEG.mSOFSegment.getComponentIds());
				sosSegment.setTableDC(0, 0);
				sosSegment.setTableAC(0, 0);
				sosSegment.setTableDC(1, 1);
				sosSegment.setTableAC(1, 1);
				sosSegment.setTableDC(2, 1);
				sosSegment.setTableAC(2, 1);
			}

			sosSegment.prepareMCU();

			int[][] mcu = new int[aJPEG.mMCUBlockCount][64];

			int[][][][] coefficients = aJPEG.mCoefficients;
			int width = aJPEG.mSOFSegment.getWidth();
			int height = aJPEG.mSOFSegment.getHeight();
			int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
			int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
			int mcuWidth = 8 * maxSamplingX;
			int mcuHeight = 8 * maxSamplingY;

			if (aCompressionType.isArithmetic())
			{
				aJPEG.mArithDCL = new int[]{0,0};
				aJPEG.mArithDCU = new int[]{1,1};
				aJPEG.mArithACK = new int[]{5,5};

				new DACSegment(aJPEG, sosSegment).encode(aOutput).log(aLog);

				if (encoder == null)
				{
					encoder = new ArithmeticEncoder(aOutput);
					encoder.jinit_encoder(aJPEG, aCompressionType.isProgressive());
				}
			}
			else
			{
				if (encoder == null)
				{
					encoder = new HuffmanEncoder(aOutput);
					encoder.jinit_encoder(aJPEG, aCompressionType.isProgressive());
				}

				if (aCompressionType == CompressionType.HuffmanOptimized || aCompressionType.isProgressive())
				{
					encoder.start_pass(aJPEG, true);

					if (aJPEG.mScanBlockCount == 1)
					{
						ComponentInfo comp = aJPEG.mComponentInfo[0];

						for (int mcuY = 0; mcuY < num_ver_mcu; mcuY++)
						{
							for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
							{
								if (mcuY < num_ver_mcu - 1 || mcuY * mcuHeight + blockY * 8 < height)
								{
									for (int mcuX = 0; mcuX < num_hor_mcu; mcuX++)
									{
										for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
										{
											if (mcuX < num_hor_mcu - 1 || mcuX * mcuWidth + blockX * 8 < width)
											{
												mcu[0] = coefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

												encoder.encode_mcu(aJPEG, mcu, true);
											}
										}
									}
								}
							}
						}
					}
					else
					{
						for (int mcuY = 0; mcuY < num_ver_mcu; mcuY++)
						{
							for (int mcuX = 0; mcuX < num_hor_mcu; mcuX++)
							{
								encoder.encode_mcu(aJPEG, coefficients[mcuY][mcuX], true);
							}
						}
					}

					encoder.finish_pass(aJPEG, true);
				}
				else
				{
					HuffmanEncoder.setupStandardHuffmanTables(aJPEG);
				}

				new DHTSegment(aJPEG).encode(aOutput).log(aLog);
			}

			sosSegment.encode(aOutput).log(aLog);

			int streamOffset = aOutput.getStreamOffset();

			encoder.start_pass(aJPEG, false);

			if (aJPEG.mScanBlockCount == 1)
			{
				ComponentInfo comp = aJPEG.mComponentInfo[0];

				for (int mcuY = 0; mcuY < num_ver_mcu; mcuY++)
				{
					for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
					{
						if (mcuY < num_ver_mcu - 1 || mcuY * mcuHeight + blockY * 8 < height)
						{
							for (int mcuX = 0; mcuX < num_hor_mcu; mcuX++)
							{
								for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
								{
									if (mcuX < num_hor_mcu - 1 || mcuX * mcuWidth + blockX * 8 < width)
									{
										mcu[0] = coefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

										encoder.encode_mcu(aJPEG, mcu, false);
									}
								}
							}
						}
					}
				}
			}
			else
			{
				for (int mcuY = 0; mcuY < num_ver_mcu; mcuY++)
				{
					for (int mcuX = 0; mcuX < num_hor_mcu; mcuX++)
					{
						encoder.encode_mcu(aJPEG, coefficients[mcuY][mcuX], false);
					}
				}
			}

			encoder.finish_pass(aJPEG, false);

			aLog.println("<image data %d bytes%s>", aOutput.getStreamOffset() - streamOffset, aCompressionType.isProgressive() ? ", progression level " + (1 + progressionLevel) : "");
		}
	}
}
