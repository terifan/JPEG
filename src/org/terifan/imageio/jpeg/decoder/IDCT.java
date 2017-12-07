package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.DQTMarkerSegment;


public interface IDCT
{
	void transform(int[] aCoefficients, DQTMarkerSegment aQuantizationTable);
}
