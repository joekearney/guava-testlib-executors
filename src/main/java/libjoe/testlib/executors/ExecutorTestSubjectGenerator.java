package libjoe.testlib.executors;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nullable;

import libjoe.testlib.executors.testers.AbstractExecutorTester;

import com.google.common.collect.testing.TestSubjectGenerator;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public abstract class ExecutorTestSubjectGenerator<E extends Executor> implements TestSubjectGenerator<E> {
//	private AbstractExecutorTester<E> currentTester;
	private ThreadFactory threadFactory;
	final Queue<E> liveExecutors = new ConcurrentLinkedQueue<E>();

	public final void setTester(AbstractExecutorTester<E> currentTester) {
//		this.currentTester = currentTester;
		this.threadFactory = new ThreadFactoryBuilder().setDaemon(true)
				.setNameFormat("executor-tester-[" + currentTester.getName() + "." + currentTester.getTestMethodName() + "]-%s")
				.setThreadFactory(new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new InterruptRecordingThread(r);
					}
				})
				.build();
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
	
	public boolean wasInterrupted(@Nullable Thread thread) {
		return thread != null && thread instanceof InterruptRecordingThread && ((InterruptRecordingThread)thread).wasInterrupted;
	}
	
	@Override
	public final E createTestSubject() {
		E executor = createExecutor(threadFactory);
		liveExecutors.add(executor);
		return executor;
	}

	public void tearDown() {
		while (!liveExecutors.isEmpty()) {
			close(liveExecutors.poll());
		}
	}
	void close(E usedExecutor) {}

	protected abstract E createExecutor(ThreadFactory threadFactory);
}
