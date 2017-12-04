package org.terifan.multimedia.jpeg;


public interface IDCT
{
	void transform(int[] aCoefficients, DQTMarkerSegment aQuantizationTable);
}
