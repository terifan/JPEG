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


	abstract void initialize(JPEG cinfo) throws IOException;

	abstract void finishPass(JPEG cinfo) throws IOException;

	abstract void startPass(JPEG cinfo) throws IOException;

	abstract boolean decodeMCU(JPEG cinfo, int[][] aCoefficients) throws IOException;
	

	public String getDecoderInfo(JPEG aJPEG)
	{
		if (aJPEG.mProgressive)
		{
			if (aJPEG.Ah == 0)
			{
				if (aJPEG.Ss == 0)
				{
					return "decode_mcu_DC_first, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al;
				}

				return "decode_mcu_AC_first, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al;
			}

			if (aJPEG.Ss == 0)
			{
				return "decode_mcu_DC_refine, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al;
			}

			return "decode_mcu_AC_refine, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al;
		}

		return "decode_mcu, bits " + aJPEG.Ss + "-" + aJPEG.Se + ", scale " + aJPEG.Al;
	}
}
