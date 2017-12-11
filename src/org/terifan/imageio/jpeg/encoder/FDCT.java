package org.terifan.imageio.jpeg.encoder;

import org.terifan.imageio.jpeg.DQTSegment;


public interface FDCT
{
	void transform(int[] aCoefficients, DQTSegment aQuantizationTable);
}
