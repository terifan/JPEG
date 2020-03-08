package org.terifan.imageio.jpeg.encoder;

import java.util.ArrayList;
import org.terifan.imageio.jpeg.JPEGConstants;


public class ProgressionScript
{
	private ArrayList<int[][]> mParams;


	public final static ProgressionScript DEFAULT = new ProgressionScript(
		  "# Initial DC scan for Y,Cb,Cr (lowest bit not sent)\n"
		+ "0,1,2: 0-0,   0, 1 ;\n"
		+ "# First AC scan: send first 5 Y AC coefficients, minus 2 lowest bits:\n"
		+ "0:     1-5,   0, 2 ;\n"
		+ "# Send all Cr,Cb AC coefficients, minus lowest bit:\n"
		+ "# (chroma data is usually too small to be worth subdividing further;\n"
		+ "#  but note we send Cr first since eye is least sensitive to Cb)\n"
		+ "2:     1-63,  0, 1 ;\n"
		+ "1:     1-63,  0, 1 ;\n"
		+ "# Send remaining Y AC coefficients, minus 2 lowest bits:\n"
		+ "0:     6-63,  0, 2 ;\n"
		+ "# Send next-to-lowest bit of all Y AC coefficients:\n"
		+ "0:     1-63,  2, 1 ;\n"
		+ "# At this point we've sent all but the lowest bit of all coefficients.\n"
		+ "# Send lowest bit of DC coefficients\n"
		+ "0,1,2: 0-0,   1, 0 ;\n"
		+ "# Send lowest bit of AC coefficients\n"
		+ "2:     1-63,  1, 0 ;\n"
		+ "1:     1-63,  1, 0 ;\n"
		+ "# Y AC lowest bit scan is last; it's usually the largest scan\n"
		+ "0:     1-63,  1, 0 ;");

	public final static ProgressionScript DC_THEN_AC = new ProgressionScript(
		  "0,1,2: 0-0,   0, 0 ;\n"
		+ "0:     1-63,  0, 0 ;\n"
		+ "2:     1-63,  0, 0 ;\n"
		+ "1:     1-63,  0, 0 ;\n");

	public final static ProgressionScript B = new ProgressionScript(
		  "0,1,2: 0-0,   0, 0 ;\n"
		+ "0:     1-5,   0, 0 ;\n"
		+ "0:     6-63,  0, 0 ;\n"
		+ "2:     1-63,  0, 0 ;\n"
		+ "1:     1-63,  0, 0 ;\n");

	public final static ProgressionScript C = new ProgressionScript(
		  "0,1,2: 0-0,   0, 1 ;\n"
		+ "0,1,2: 0-0,   1, 0 ;\n"
		+ "0:     1-5,   0, 0 ;\n"
		+ "0:     6-63,  0, 0 ;\n"
		+ "2:     1-63,  0, 0 ;\n"
		+ "1:     1-63,  0, 0 ;\n");

	public final static ProgressionScript D = new ProgressionScript(
		  "0,1,2: 0-0,   0, 1 ;\n"
		+ "0:     1-5,   0, 2 ;\n"
		+ "2:     1-63,  0, 1 ;\n"
		+ "1:     1-63,  0, 1 ;\n"
		+ "0:     6-63,  0, 2 ;\n"
		+ "0:     1-63,  2, 1 ;\n"
		+ "0,1,2: 0-0,   1, 0 ;\n"
		+ "2:     1-63,  1, 0 ;\n"
		+ "1:     1-63,  1, 0 ;\n"
		+ "0:     1-63,  1, 0 ;");

	public final static ProgressionScript E = new ProgressionScript(
		  "# Interleaved DC scan for Y,Cb,Cr:\n"
		+ "0,1,2: 0-0,   0, 1 ;\n"
		+ "0,1,2: 0-0,   1, 0 ;\n"
		+ "# AC scans:\n"
		+ "0:     1-2,   0, 0 ;	# First two Y AC coefficients\n"
		+ "0:     3-5,   0, 0 ;	# Three more\n"
		+ "1:     1-63,  0, 0 ;	# All AC coefficients for Cb\n"
		+ "2:     1-63,  0, 0 ;	# All AC coefficients for Cr\n"
		+ "0:     6-9,   0, 0 ;	# More Y coefficients\n"
		+ "0:     10-63, 0, 0 ;	# Remaining Y coefficients");


	public ProgressionScript(String aScript)
	{
		mParams = new ArrayList<>();

		aScript = aScript.replace('\r', '\n').trim();

		while (!aScript.isEmpty())
		{
			if (aScript.startsWith("#"))
			{
				int i = aScript.indexOf("\n");

				if (i == -1)
				{
					break;
				}

				aScript = aScript.substring(i + 1);
			}
			else
			{
				int i = aScript.indexOf(";");

				if (i == -1)
				{
					parseLine(aScript);
					break;
				}

				parseLine(aScript.substring(0, i));
				aScript = aScript.substring(i + 1);
			}

			aScript = aScript.trim();
		}
	}


	private void parseLine(String aLine) throws NumberFormatException
	{
		String line = aLine.replaceAll("[\\t\\-,\\._]+", " ").replaceAll("\\s+", " ");

		if (line.contains("#"))
		{
			throw new IllegalArgumentException("Bad progression script: found comment in parameter line, missing semicolon? On line: " + aLine);
		}

		int i = line.indexOf(":");

		if (i == -1)
		{
			throw new IllegalArgumentException("Bad progression script: comma separating components and scan parameters not found: " + aLine);
		}

		String[] compParams = line.substring(0, i).trim().split(" ");
		String[] scanParams = line.substring(i + 1).trim().split(" ");

		if (compParams.length > JPEGConstants.MAX_CHANNELS)
		{
			throw new IllegalArgumentException("Bad progression script: max " + JPEGConstants.MAX_CHANNELS + " component params: " + aLine);
		}
		if (scanParams.length != 4)
		{
			throw new IllegalArgumentException("Bad progression script: 4 scan params required: " + aLine);
		}

		int[] compParamsInt = new int[compParams.length];
		int[] scanParamsInt = new int[4];

		for (int j = 0; j < compParamsInt.length; j++)
		{
			compParamsInt[j] = Integer.parseInt(compParams[j]);
		}
		for (int j = 0; j < scanParamsInt.length; j++)
		{
			scanParamsInt[j] = Integer.parseInt(scanParams[j]);
		}

		mParams.add(new int[][]{compParamsInt, scanParamsInt});
	}


	public ArrayList<int[][]> getParams()
	{
		return mParams;
	}
}
