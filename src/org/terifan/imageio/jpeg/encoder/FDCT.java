package org.terifan.imageio.jpeg.encoder;

import org.terifan.imageio.jpeg.QuantizationTable;


public interface FDCT
{
	void transform(int[] aCoefficients, QuantizationTable aQuantizationTable);
}
