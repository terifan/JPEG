package org.terifan.imageio.jpeg;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class FixedThreadExecutor
{
	private ExecutorService mExecutorService;


	public FixedThreadExecutor(float aFactor)
	{
		int available = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		int threads = Math.max(1, Math.min(available, (int)Math.round(available * aFactor)));
		mExecutorService = new ThreadPoolExecutor(threads, threads, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
	}


	public void submit(Runnable aRunnable)
	{
		mExecutorService.submit(aRunnable);
	}


	public synchronized void shutdown()
	{
		if (mExecutorService != null)
		{
			try
			{
				mExecutorService.shutdown();
				mExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
			}
			finally
			{
				mExecutorService = null;
			}
		}
	}
}
