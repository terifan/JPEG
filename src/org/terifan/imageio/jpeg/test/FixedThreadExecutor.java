package org.terifan.imageio.jpeg.test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Helper class replacing Executors.newFixedThreadPool()
 */
public class FixedThreadExecutor<T> implements AutoCloseable
{
	private LinkedBlockingQueue mBlockingQueue;
	private ExecutorService mExecutorService;
	private int mThreads;
	private int mQueueSizeLimit;
	private OnCompletion mOnCompletion;
	private HashMap<CallableTask, AtomicReference> mResultReceivers;


	/**
	 * Create a new executor
	 *
	 * @param aNumThreads a positive number equals number of threads to use, zero or a negative number results in total available processors
	 * minus provided number.
	 */
	public FixedThreadExecutor(int aNumThreads)
	{
		if (aNumThreads > 0)
		{
			mThreads = aNumThreads;
		}
		else
		{
			mThreads = Math.max(1, ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() + aNumThreads);
		}

		mBlockingQueue = new LinkedBlockingQueue<>();
		mQueueSizeLimit = Integer.MAX_VALUE;
		mResultReceivers = new HashMap<>();
	}


	/**
	 * Create a new executor
	 *
	 * @param aThreads number of threads expressed as a number between 0 and 1 out of total available CPUs
	 */
	public FixedThreadExecutor(float aThreads)
	{
		if (aThreads < 0 || aThreads > 1)
		{
			throw new IllegalArgumentException();
		}

		int cpu = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();

		mThreads = Math.max(1, Math.min(cpu, (int)Math.round(cpu * aThreads)));
		mBlockingQueue = new LinkedBlockingQueue<>();
		mQueueSizeLimit = Integer.MAX_VALUE;
		mResultReceivers = new HashMap<>();
	}


	/**
	 * @see java.util.concurrent.ExecutorService#shutdown
	 */
	public void shutdown()
	{
		if (mExecutorService != null)
		{
			mExecutorService.shutdown();
		}
	}


	/**
	 * @see java.util.concurrent.ExecutorService#shutdownNow
	 */
	public List<Runnable> shutdownNow()
	{
		if (mExecutorService != null)
		{
			return mExecutorService.shutdownNow();
		}

		return new ArrayList<>();
	}


	/**
	 * Submit a task, this method may block if the queue size exceeds the limit.
	 *
	 * @see java.util.concurrent.ExecutorService#submit
	 */
	public void submit(RunnableTask aRunnable)
	{
		doSubmit(init(), aRunnable);
	}


	public void call(AtomicReference aResultReceiver, CallableTask aRunnable)
	{
		mResultReceivers.put(aRunnable, aResultReceiver);
		doSubmit(init(), aRunnable);
	}


//	public void submit(Callable aCallable)
//	{
//		doSubmit(init(), aCallable);
//	}


//	/**
//	 * Submit a task, this method may block if the queue size exceeds the limit.
//	 *
//	 * @see java.util.concurrent.ExecutorService#submit
//	 */
//	public void submit(T... aRunnables)
//	{
//		ExecutorService service = init();
//
//		for (T r : aRunnables)
//		{
//			doSubmit(service, r);
//		}
//	}
//
//
//	/**
//	 * Submit a task, this method may block if the queue size exceeds the limit.
//	 *
//	 * @see java.util.concurrent.ExecutorService#submit
//	 */
//	public void submit(Iterable<? extends T> aRunnables)
//	{
//		ExecutorService service = init();
//
//		for (T r : aRunnables)
//		{
//			doSubmit(service, r);
//		}
//	}


	private void doSubmit(ExecutorService aService, Object aRunnable)
	{
		try
		{
			synchronized (FixedThreadExecutor.class)
			{
				while (mBlockingQueue.size() >= mQueueSizeLimit)
				{
					FixedThreadExecutor.class.wait();
				}

				if (aRunnable instanceof RunnableTask)
				{
					aService.submit(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								((RunnableTask)aRunnable).run();
							}
							catch (Exception e)
							{
							}
						}
					});
				}
				else if (aRunnable instanceof CallableTask)
				{
					aService.submit(new Callable()
					{
						@Override
						public Object call()
						{
							try
							{
								return ((CallableTask)aRunnable).run();
							}
							catch (Exception e)
							{
								return e;
							}
						}
					});
				}
			}
		}
		catch (InterruptedException e)
		{
		}
	}


	@Override
	public void close()
	{
		close(Long.MAX_VALUE);
	}


	private synchronized boolean close(long aWaitMillis)
	{
		if (mExecutorService != null)
		{
			try
			{
				mExecutorService.shutdown();

				mExecutorService.awaitTermination(aWaitMillis, TimeUnit.MILLISECONDS);

				return false;
			}
			catch (InterruptedException e)
			{
				return true;
			}
			finally
			{
				mExecutorService = null;
			}
		}

		return false;
	}


	private synchronized ExecutorService init()
	{
		if (mExecutorService == null)
		{
			mExecutorService = new ThreadPoolExecutor(mThreads, mThreads, 0L, TimeUnit.MILLISECONDS, (BlockingQueue<Runnable>)mBlockingQueue)
			{
				@Override
				protected void afterExecute(Runnable aRunnable, Throwable aThrowable)
				{
					super.afterExecute(aRunnable, aThrowable);

					synchronized (FixedThreadExecutor.class)
					{
						FixedThreadExecutor.class.notify();
					}

					if (mOnCompletion != null)
					{
						mOnCompletion.onCompletion((Future)aRunnable);
					}

//					if (aThrowable == null && aRunnable instanceof Future<?>)
//					{
//						try
//						{
//							Object result = ((Future<?>)aRunnable).get();
//						}
//						catch (CancellationException ce)
//						{
//							aThrowable = ce;
//						}
//						catch (ExecutionException ee)
//						{
//							aThrowable = ee.getCause();
//						}
//						catch (InterruptedException ie)
//						{
//							Thread.currentThread().interrupt(); // ignore/reset
//						}
//					}

					if (aThrowable != null)
					{
						aThrowable.printStackTrace(System.err);
					}
				}
			};
		}

		return mExecutorService;
	}


	public LinkedBlockingQueue<T> getBlockingQueue()
	{
		return mBlockingQueue;
	}


	public int getQueueSizeLimit()
	{
		return mQueueSizeLimit;
	}


	/**
	 * Sets how many items the blocking queue will contain before the submit methods start blocking.
	 */
	public FixedThreadExecutor<T> setQueueSizeLimit(int aQueueSizeLimit)
	{
		mQueueSizeLimit = aQueueSizeLimit;
		return this;
	}


	public OnCompletion getOnCompletion()
	{
		return mOnCompletion;
	}


	public FixedThreadExecutor<T> setOnCompletion(OnCompletion aOnCompletion)
	{
		mOnCompletion = aOnCompletion;
		return this;
	}


	@FunctionalInterface
	public interface RunnableTask
	{
		void  run() throws Exception;
	}


	@FunctionalInterface
	public interface CallableTask<U>
	{
		U run() throws Exception;
	}


	@FunctionalInterface
	public interface OnCompletion
	{
		void onCompletion(Future aFuture);
	}
}
