package org.terifan.imageio.jpeg;

import java.io.PrintStream;


public class Log
{
	protected PrintStream mPrintStream;
	protected boolean mDetailed;


	public Log()
	{
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


	public boolean isDetailed()
	{
		return mDetailed;
	}


	public PrintStream getPrintStream()
	{
		return mPrintStream;
	}


	public void println(Object aText)
	{
		mPrintStream.println(aText);
	}


	public void print(String aText)
	{
		mPrintStream.print(aText);
	}


	public void printf(String aText, Object... aParams)
	{
		mPrintStream.printf(aText, aParams);
	}


	public void println()
	{
		mPrintStream.println();
	}
}
