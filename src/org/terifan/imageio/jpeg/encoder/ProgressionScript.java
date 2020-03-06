package org.terifan.imageio.jpeg.encoder;

import java.util.ArrayList;
import org.terifan.imageio.jpeg.JPEGConstants;


public class ProgressionScript
{
	private ArrayList<int[][]> mParams;


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
