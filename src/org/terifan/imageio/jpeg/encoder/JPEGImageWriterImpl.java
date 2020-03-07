package org.terifan.imageio.jpeg.encoder;

import java.io.IOException;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.APP2Segment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DHTSegment;
import org.terifan.imageio.jpeg.DQTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.Log;
import org.terifan.imageio.jpeg.SOSSegment;


public class JPEGImageWriterImpl
{
	public void create(JPEG aJPEG, BitOutputStream aOutput, Log aLog) throws IOException
	{
		aOutput.writeInt16(JPEGConstants.SOI);

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
		aOutput.writeInt16(JPEGConstants.EOI);
		aLog.println("EOI");
	}


	public void encode(JPEG aJPEG, BitOutputStream aOutput, Log aLog, ProgressionScript aProgressionScript) throws IOException
	{
		if (aJPEG.mProgressive && aProgressionScript == null)
		{
			aProgressionScript = new ProgressionScript(ProgressionScript.DC_THEN_AC);
		}

		aJPEG.num_hor_mcu = aJPEG.mSOFSegment.getHorMCU();
		aJPEG.num_ver_mcu = aJPEG.mSOFSegment.getVerMCU();

		Encoder encoder = null;

		int progressionLevels = aJPEG.mProgressive ? aProgressionScript.getParams().size() : 1;

		for (int progressionLevel = 0; progressionLevel < progressionLevels; progressionLevel++)
		{
			SOSSegment sosSegment;

			if (aJPEG.mProgressive)
			{
				int[][] params = aProgressionScript.getParams().get(progressionLevel);

				aJPEG.Ss = params[1][0];
				aJPEG.Se = params[1][1];
				aJPEG.Ah = params[1][2];
				aJPEG.Al = params[1][3];

				int[] id = new int[params[0].length];
				for (int i = 0; i < params[0].length; i++)
				{
					id[i] = aJPEG.mSOFSegment.getComponent(params[0][i]).getComponentId();
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

				sosSegment.prepareMCU();
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

				sosSegment.prepareMCU();
			}

			int[][] mcu = new int[aJPEG.blocks_in_MCU][64];

			int maxSamplingX = aJPEG.mSOFSegment.getMaxHorSampling();
			int maxSamplingY = aJPEG.mSOFSegment.getMaxVerSampling();
			int mcuWidth = 8 * maxSamplingX;
			int mcuHeight = 8 * maxSamplingY;

			if (aJPEG.mArithmetic)
			{
				aJPEG.arith_dc_L = new int[]{0,0};
				aJPEG.arith_dc_U = new int[]{1,1};
				aJPEG.arith_ac_K = new int[]{5,5};

				new DACSegment(aJPEG, sosSegment).encode(aOutput).log(aLog);

				if (encoder == null)
				{
					encoder = new ArithmeticEncoder(aOutput);
					encoder.jinit_encoder(aJPEG);
				}
			}
			else
			{
				if (encoder == null)
				{
					encoder = new HuffmanEncoder(aOutput);
					encoder.jinit_encoder(aJPEG);
				}

				if (aJPEG.mOptimizedHuffman || aJPEG.mProgressive)
				{
					encoder.start_pass(aJPEG, true);

					if (aJPEG.comps_in_scan == 1)
					{
						ComponentInfo comp = aJPEG.cur_comp_info[0];

						for (int mcuY = 0; mcuY < aJPEG.num_ver_mcu; mcuY++)
						{
							for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
							{
								if (mcuY < aJPEG.num_ver_mcu - 1 || mcuY * mcuHeight + blockY * 8 < aJPEG.mSOFSegment.getHeight())
								{
									for (int mcuX = 0; mcuX < aJPEG.num_hor_mcu; mcuX++)
									{
										for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
										{
											if (mcuX < aJPEG.num_hor_mcu - 1 || mcuX * mcuWidth + blockX * 8 < aJPEG.mSOFSegment.getWidth())
											{
												mcu[0] = aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

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
						for (int mcuY = 0; mcuY < aJPEG.num_ver_mcu; mcuY++)
						{
							for (int mcuX = 0; mcuX < aJPEG.num_hor_mcu; mcuX++)
							{
								encoder.encode_mcu(aJPEG, aJPEG.mCoefficients[mcuY][mcuX], true);
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

			if (aJPEG.comps_in_scan == 1)
			{
				ComponentInfo comp = aJPEG.cur_comp_info[0];

				for (int mcuY = 0; mcuY < aJPEG.num_ver_mcu; mcuY++)
				{
					for (int blockY = 0; blockY < comp.getVerSampleFactor(); blockY++)
					{
						if (mcuY < aJPEG.num_ver_mcu - 1 || mcuY * mcuHeight + blockY * 8 < aJPEG.mSOFSegment.getHeight())
						{
							for (int mcuX = 0; mcuX < aJPEG.num_hor_mcu; mcuX++)
							{
								for (int blockX = 0; blockX < comp.getHorSampleFactor(); blockX++)
								{
									if (mcuX < aJPEG.num_hor_mcu - 1 || mcuX * mcuWidth + blockX * 8 < aJPEG.mSOFSegment.getWidth())
									{
										mcu[0] = aJPEG.mCoefficients[mcuY][mcuX][comp.getComponentBlockOffset() + comp.getHorSampleFactor() * blockY + blockX];

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
				for (int mcuY = 0; mcuY < aJPEG.num_ver_mcu; mcuY++)
				{
					for (int mcuX = 0; mcuX < aJPEG.num_hor_mcu; mcuX++)
					{
						encoder.encode_mcu(aJPEG, aJPEG.mCoefficients[mcuY][mcuX], false);
					}
				}
			}

			encoder.finish_pass(aJPEG, false);

			aLog.println("<output " + (aOutput.getStreamOffset() - streamOffset) + " bytes>");
		}
	}
}
