package org.terifan.imageio.jpeg.encoder;

import java.io.IOException;
import org.terifan.imageio.jpeg.JPEG;


public interface Encoder 
{
	boolean encode_mcu(JPEG cinfo, int[][] MCU_data, boolean gather_statistics) throws IOException;
	void jinit_encoder(JPEG cinfo) throws IOException;
	void start_pass(JPEG cinfo, boolean gather_statistics) throws IOException;
	void finish_pass(JPEG cinfo, boolean gather_statistics) throws IOException;
}
