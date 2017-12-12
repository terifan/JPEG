package org.terifan.imageio.jpeg.encoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.terifan.imageio.jpeg.APP0Segment;
import org.terifan.imageio.jpeg.ComponentInfo;
import org.terifan.imageio.jpeg.DACSegment;
import org.terifan.imageio.jpeg.DQTSegment;
import org.terifan.imageio.jpeg.JPEG;
import org.terifan.imageio.jpeg.JPEGConstants;
import org.terifan.imageio.jpeg.QuantizationTable;
import org.terifan.imageio.jpeg.SOFSegment;
import org.terifan.imageio.jpeg.SOSSegment;
import org.terifan.imageio.jpeg.decoder.ArithEntropyState;


public class JPEGImageWriter
{
	private BitOutputStream mBitStream;
	private JPEG mJPEG;


	public void write(BufferedImage aImage, int aQuality, OutputStream aOutputStream) throws IOException
	{
		mJPEG = new JPEG();

		mJPEG.mQuantizationTables = new QuantizationTable[2];
		mJPEG.mQuantizationTables[0] = QuantizationTableFactory.buildQuantTable(aQuality, 0);
		mJPEG.mQuantizationTables[1] = QuantizationTableFactory.buildQuantTable(aQuality, 1);
		mJPEG.mArithmetic = true;

		mJPEG.arith_dc_L = new int[]{0,0};
		mJPEG.arith_dc_U = new int[]{1,1};
		mJPEG.arith_ac_K = new int[]{5,5};

		mBitStream = new BitOutputStream(aOutputStream);

		mBitStream.writeInt16(JPEGConstants.SOI);

		new APP0Segment(mJPEG).write(mBitStream);

		new DQTSegment(mJPEG).write(mBitStream);

		ComponentInfo lu, cb, cr;

		SOFSegment mSOFSegment = new SOFSegment(mJPEG, aImage.getWidth(), aImage.getHeight(), 8,
			lu=new ComponentInfo(ComponentInfo.Y, 1, 0, 2, 2),
			cb=new ComponentInfo(ComponentInfo.CB, 2, 1, 1, 1),
			cr=new ComponentInfo(ComponentInfo.CR, 3, 1, 1, 1)
		).write(mBitStream);

		new DACSegment(mJPEG).write(mBitStream);

		mJPEG.comps_in_scan = 3;
		mJPEG.cur_comp_info = new ComponentInfo[]{lu,cb,cr};
		mJPEG.Ss = 0;
		mJPEG.Se = 63;
		mJPEG.Ah = 0;
		mJPEG.Al = 0;

		mJPEG.cur_comp_info[0].setTableDC(0);
		mJPEG.cur_comp_info[0].setTableAC(0);
		mJPEG.cur_comp_info[1].setTableDC(1);
		mJPEG.cur_comp_info[1].setTableAC(1);
		mJPEG.cur_comp_info[2].setTableDC(1);
		mJPEG.cur_comp_info[2].setTableAC(1);

		new SOSSegment(mJPEG).write(mBitStream);

		mJPEG.blocks_in_MCU = 6;

		mJPEG.MCU_membership = new int[mJPEG.blocks_in_MCU];
		mJPEG.MCU_membership[0] = 0;
		mJPEG.MCU_membership[1] = 0;
		mJPEG.MCU_membership[2] = 0;
		mJPEG.MCU_membership[3] = 0;
		mJPEG.MCU_membership[4] = 1;
		mJPEG.MCU_membership[5] = 2;

		int maxSamplingX = mSOFSegment.getMaxHorSampling();
		int maxSamplingY = mSOFSegment.getMaxVerSampling();
		int numHorMCU = mSOFSegment.getHorMCU();
		int numVerMCU = mSOFSegment.getVerMCU();


		FDCT fdct = new FDCTFloat();

		int[][][][] buffer = new int[numVerMCU][numHorMCU][mJPEG.blocks_in_MCU][64];

		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
				for (int component = 0, blockIndex = 0; component < mJPEG.num_components; component++)
				{
					ComponentInfo comp = mSOFSegment.getComponent(component);
					int samplingX = comp.getHorSampleFactor();
					int samplingY = comp.getVerSampleFactor();

					QuantizationTable quantizationTable = mJPEG.mQuantizationTables[comp.getQuantizationTableId()];

					for (int blockY = 0; blockY < samplingY; blockY++)
					{
						for (int blockX = 0; blockX < samplingX; blockX++, blockIndex++)
						{
							int x = mcuX * maxSamplingX * 8 + blockX * 8;
							int y = mcuY * maxSamplingY * 8 + blockY * 8;

							aImage.getRGB(x, y, 8, 8, buffer[mcuY][mcuX][blockIndex], 0, aImage.getWidth());

							fdct.transform(buffer[mcuY][mcuX][blockIndex], quantizationTable);
						}
					}
				}
			}
		}



		ArithmeticEncoder encoder = new ArithmeticEncoder(mBitStream);
		encoder.jinit_encoder(mJPEG);
		encoder.start_pass(mJPEG, false);

		for (int mcuY = 0; mcuY < numVerMCU; mcuY++)
		{
			for (int mcuX = 0; mcuX < numHorMCU; mcuX++)
			{
//				for (int component = 0, blockIndex = 0; component < mJPEG.num_components; component++)
//				{
//					ComponentInfo comp = mJPEG.cur_comp_info[component];
//					int samplingX = comp.getHorSampleFactor();
//					int samplingY = comp.getVerSampleFactor();
//
//					for (int blockY = 0; blockY < samplingY; blockY++)
//					{
//						for (int blockX = 0; blockX < samplingX; blockX++, blockIndex++)
//						{
//
//						}
//					}
//				}

				encoder.encode_mcu(mJPEG, buffer[mcuY][mcuX]);
			}
		}

		encoder.finish_pass(mJPEG);

		mBitStream.writeInt16(JPEGConstants.EOI);

		System.out.println("+++"+mBitStream.getStreamOffset());
	}
}
