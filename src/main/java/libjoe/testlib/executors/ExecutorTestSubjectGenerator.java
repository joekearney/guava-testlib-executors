package libjoe.testlib.executors;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nullable;

import libjoe.testlib.executors.testers.AbstractExecutorTester;

import com.google.common.collect.testing.TestSubjectGenerator;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public abstract class ExecutorTestSubjectGenerator<E extends Executor> implements TestSubjectGenerator<E> {
	static final int UNASSIGNED = -1;
	
	//	private AbstractExecutorTester<E> currentTester;
	private ThreadFactory threadFactory;
	final Queue<E> liveExecutors = new ConcurrentLinkedQueue<E>();

    private int maxCapacity = UNASSIGNED;
    private int concurrencyLevel = UNASSIGNED;

	public final void setTester(AbstractExecutorTester<E> currentTester) {
//		this.currentTester = currentTester;
		this.threadFactory = new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat("executor-tester-[" + currentTester.getName() + "." + currentTester.getTestMethodName() + "]-%s")
				.setThreadFactory(new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new InterruptRecordingThread(r);
					}
				})
				.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						// ignore
					}
				})
				.build();
	}
    final void setMaxCapicity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    final void setConcurrencyLevel(int concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }
	
	@Override
	public final E createTestSubject() {
		E executor = createExecutor(threadFactory);
		liveExecutors.add(executor);
		return executor;
	}

	public final void tearDown() {
		while (!liveExecutors.isEmpty()) {
			close(liveExecutors.poll());
		}
	}
	final void close(E usedExecutor) {
		if (usedExecutor != null && usedExecutor instanceof ExecutorService) {
			((ExecutorService) usedExecutor).shutdownNow();
		}
	}

	protected abstract E createExecutor(ThreadFactory threadFactory);
	public final int getMaxCapacity() {
        return maxCapacity;
    }
	public final int getConcurrencyLevel() {
        return concurrencyLevel;
    }
	
	private static final class InterruptRecordingThread extends Thread {
		private volatile boolean wasInterrupted = false;
		
		InterruptRecordingThread(Runnable r) {
			super(r);
		}

		@Override
		public void interrupt() {
			wasInterrupted = true;
			super.interrupt();
		}
	}
	public static boolean wasInterrupted(@Nullable Thread thread) {
		return thread != null && thread instanceof InterruptRecordingThread && ((InterruptRecordingThread)thread).wasInterrupted;
	}
}
