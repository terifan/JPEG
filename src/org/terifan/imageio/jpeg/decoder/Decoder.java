package org.terifan.imageio.jpeg.decoder;

import java.io.IOException;
import org.terifan.imageio.jpeg.JPEG;


public abstract class Decoder
{
	protected BitInputStream mBitStream;


	public Decoder(BitInputStream aBitStream)
	{
		mBitStream = aBitStream;
	}


	abstract void initialize(JPEG cinfo, boolean aProgressive) throws IOException;

	abstract void finishPass(JPEG cinfo) throws IOException;

	abstract void startPass(JPEG cinfo) throws IOException;

	abstract boolean decodeMCU(JPEG cinfo, int[][] aCoefficients) throws IOException;


//	public String getDecoderInfo(JPEG aJPEG)
//	{
//		String fn = "decode_mcu";
//
//		if (aJPEG.mProgressive)
//		{
//			if (aJPEG.Ss == 0)
//			{
//				fn += "_DC";
//			}
//			else
//			{
//				fn += "_AC";
//			}
//			if (aJPEG.Ah == 0)
//			{
//				fn += "_first";
//			}
//			else
//			{
//				fn += "_refine";
//			}
//		}
//
//		return fn + ", bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al;
//	}
}
