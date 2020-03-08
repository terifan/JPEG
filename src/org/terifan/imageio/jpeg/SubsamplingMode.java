package org.terifan.imageio.jpeg;


public enum SubsamplingMode
{
	/** full horizontal resolution, full vertical resolution, best quality */
	_444("4:4:4", "1x1,1x1,1x1", "full horizontal resolution, full vertical resolution", new int[][]{{1,1},{1,1},{1,1}}),
	/** 1/2 horizontal resolution, full vertical resolution, average */
	_422("4:2:2", "2x1,1x1,1x1", "½ horizontal resolution, full vertical resolution", new int[][]{{2,1},{1,1},{1,1}}),
	/** 1/2 horizontal resolution, 1/2 vertical resolution, best compression */
	_420("4:2:0", "2x2,1x1,1x1", "½ horizontal resolution, ½ vertical resolution", new int[][]{{2,2},{1,1},{1,1}}),
	/** 1/4 horizontal resolution, full vertical resolution */
	_411("4:1:1", "4x1,1x1,1x1", "¼ horizontal resolution, full vertical resolution", new int[][]{{4,1},{1,1},{1,1}}),
	/** full horizontal resolution, 1/2 vertical resolution */
	_440("4:4:0", "1x2,1x1,1x1", "full horizontal resolution, ½ vertical resolution", new int[][]{{2,2},{2,1},{2,1}}),
	;

	// "4:1:0" "4x1,2x1,2x1"

	private final String mName;
	private final String mDescription;
	private final int[][] mSamplingFactors;
	private final String mLogic;


	private SubsamplingMode(String aName, String aLogic, String aDescription, int[][] aSamplingFactors)
	{
		mName = aName;
		mLogic = aLogic;
		mDescription = aDescription;
		mSamplingFactors = aSamplingFactors;
	}


	public String getLogic()
	{
		return mLogic;
	}


	public int[][] getSamplingFactors()
	{
		return mSamplingFactors;
	}


	public String getDescription()
	{
		return mDescription;
	}


	@Override
	public String toString()
	{
		return mName;
	}
}
