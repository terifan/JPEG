package org.terifan.imageio.jpeg.decoder;

import org.terifan.imageio.jpeg.QuantizationTable;


public interface IDCT
{
	void transform(int[] aCoefficients, QuantizationTable aQuantizationTable);
}
