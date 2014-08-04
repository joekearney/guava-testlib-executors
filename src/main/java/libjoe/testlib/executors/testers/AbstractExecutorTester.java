package libjoe.testlib.executors.testers;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.AssertionFailedError;
import libjoe.testlib.executors.ExecutorTestSubjectGenerator;

import com.google.common.collect.testing.AbstractTester;

public class AbstractExecutorTester<E extends Executor> extends AbstractTester<ExecutorTestSubjectGenerator<E>> {
	@Override
	public ExecutorTestSubjectGenerator<E> getSubjectGenerator() {
		return super.getSubjectGenerator();
	}

	@Override
	public void setUp() throws Exception {
		getSubjectGenerator().setTester(this);
	}

	@Override
	public void tearDown() throws Exception {
		getSubjectGenerator().tearDown();
	}

	protected final TimeUnit getTimeoutUnit() {
		return TimeUnit.SECONDS;
	}

	protected final long getTimeoutDuration() {
		return 1;
	}
	
	protected static final Object RETURN_VALUE = new Object();
	
	protected final Callable<Object> callableFor(Runnable runnable) {
		return Executors.callable(runnable, RETURN_VALUE);
	}

	protected final <T> void assertThrows(Future<T> future) {
		try {
			future.get(getTimeoutDuration(), getTimeoutUnit());
			fail("Should have thrown");
		} catch (ExecutionException e) {
			// pass
		} catch (Throwable e) {
			AssertionFailedError afe = new AssertionFailedError("Threw wrong type of exception. Expected an ExecutionException.");
			afe.initCause(e);
			throw afe;
		}
	}

	private final class ThrowingRunnable extends LoggingRunnable {
		@Override
		void doRun() {
			throw new RuntimeException("test exception, expected");
		}
	}

	private final class NoopRunnable extends LoggingRunnable {
		@Override
		void doRun() {
		}
	}

	public abstract class LoggingRunnable implements Runnable {
		private volatile Thread runningThread = null;
		private volatile boolean ran = false;
		
		@Override
		public final synchronized void run() {
			ran = true;
			Thread thisThread = Thread.currentThread();
			checkState(runningThread == null, "Runnable should be executed only once. Already executed on thread[%s], tried again on thread [%s]", runningThread, thisThread.getName());
			runningThread = thisThread;
			System.out.println("Running [" + getClass().getSimpleName() + "] in thread[" + thisThread.getName() + "]");
			doRun();
		}

		abstract void doRun();

		public final boolean wasInterrupted() {
			return getSubjectGenerator().wasInterrupted(runningThread);
		}
		public final boolean wasRun() {
			return ran;
		}
	}

	/** Creates a {@link Runnable} that does nothing. */
	protected NoopRunnable noopRunnable() {
		return new NoopRunnable();
	}

	/** Creates a {@link Runnable} that does nothing. */
	protected ThrowingRunnable throwingRunnable() {
		return new ThrowingRunnable();
	}

	protected RunnableWithLatch newRunnableWithLatch() {
		return new RunnableWithLatch();
	}
	public final class RunnableWithLatch extends LoggingRunnable {
		private final CountDownLatch latch = new CountDownLatch(1);

		@Override
		void doRun() {
			latch.countDown();
		}

		public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
			return latch.await(timeout, unit);
		}
	}
	protected RunnableWithBarrier newRunnableWithBarrier(int parties) {
		return new RunnableWithBarrier(parties);
	}
	protected final void checkFutureAfterCompleted(LoggingRunnable task, Future<?> future, Object expected) throws InterruptedException, ExecutionException,
			TimeoutException {
		Object result = future.get(getTimeoutDuration(), getTimeoutUnit());
		assertThat("Task should have been executed, but it didn't run.", task.wasRun());
		assertThat("Future should be isDone() after get() returns", future.isDone());
		assertThat("Future should not be isCancelled() after get() returns", !future.isCancelled());
		assertThat("Future should return expected value", result, is(sameInstance(expected)));
	}

	protected final void checkFutureAfterExecutionException(Future<?> future) {
		assertThrows(future);
		assertThat("Future should be isDone() after get() throws", future.isDone());
		assertThat("Future should not be isCancelled() after get() throws", !future.isCancelled());
	}

	protected final void checkTaskRan(LoggingRunnable runnable) {
		assertThat("Task should have been executed, but it didn't run.", runnable.wasRun());
	}

	protected final void cancelFutureAndCheck(RunnableWithBarrier task, Future<?> future, boolean mayInterruptIfRunning) throws InterruptedException, ExecutionException, TimeoutException {
		boolean cancelled = future.cancel(mayInterruptIfRunning);
		try {
			future.get(getTimeoutDuration(), getTimeoutUnit());
			fail("Cancelled task should have thrown cancellation exception");
		} catch (CancellationException e) {
			// pass
		}
		assertThat("Future should be isCancelled() after cancellation", future.isCancelled());
		if (cancelled) {
			if (mayInterruptIfRunning) {
				assertThat("Runnable should have been interrupted", task.wasInterrupted());
			} else {
				assertThat("Runnable should not have been interrupted", !task.wasInterrupted());
			}
		}
	}

	public final class RunnableWithBarrier extends LoggingRunnable {
		private final CyclicBarrier barrier;
		
		RunnableWithBarrier(int parties) {
			barrier = new CyclicBarrier(parties);
		}
		
		@Override
		void doRun() {
			try {
				barrier.await(getTimeoutDuration(), getTimeoutUnit());
			} catch (InterruptedException e) {
				// reassert. TODO remove this when we get centralised interrupt monitoring
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			} catch (BrokenBarrierException | TimeoutException e) {
				throw new RuntimeException(e);
			}
		}

		public void await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
			barrier.await(timeout, unit);
		}

		public void resetBarrier() {
			barrier.reset();
		}
	}
}
