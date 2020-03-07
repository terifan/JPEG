package org.terifan.imageio.jpeg;

import java.io.PrintStream;


public class Log
{
	protected PrintStream mPrintStream;
	protected boolean mDetailed;


	public Log()
	{
	}


	public Log(PrintStream aPrintStream, boolean aDetailed)
	{
		mPrintStream = aPrintStream;
		mDetailed = aDetailed;
	}


	public boolean isDetailed()
	{
		return mDetailed;
	}


	public Log setDetailed(boolean aDetailed)
	{
		mDetailed = aDetailed;
		return this;
	}


	public Log setPrintStream(PrintStream aPrintStream)
	{
		mPrintStream = aPrintStream;
		return this;
	}


	public void print(String aText, Object... aParams)
	{
		if (mPrintStream != null)
		{
			mPrintStream.printf(aText, aParams);
		}
	}


	public void println(String aText, Object... aParams)
	{
		if (mPrintStream != null)
		{
			mPrintStream.printf(aText, aParams);
			mPrintStream.println();
		}
	}
}
