package org.terifan.imageio.jpeg.encoder;

import org.terifan.imageio.jpeg.DQTMarkerSegment;


public interface FDCT
{
	void transform(int[] aCoefficients, DQTMarkerSegment aQuantizationTable);

	void transform(int[] aCoefficients);
}
