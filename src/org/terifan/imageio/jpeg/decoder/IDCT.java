package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.DQTSegment;


public interface IDCT
{
	void transform(int[] aCoefficients, DQTSegment aQuantizationTable);
}
