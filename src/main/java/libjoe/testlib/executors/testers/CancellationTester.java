package libjoe.testlib.executors.testers;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CancellationTester<E extends ExecutorService> extends AbstractExecutorTester<E> {
	public void testSubmitCancelRunningRunnable_interrupt() throws Exception {
		RunnableWithBarrier runnable = newRunnableWithBarrier(2);
		try {
			Future<?> future = getSubjectGenerator().createTestSubject().submit(runnable);
			future.cancel(true);
			checkFutureAfterCancellation(runnable, future, true);
		} finally {
			runnable.resetBarrier(); // just in case
		}
	}

	protected void checkFutureAfterCancellation(LoggingRunnable runnable, Future<?> future, boolean mayInterruptOnCancel) {
		assertThat("Future should be isCancelled() after cancellation", future.isCancelled());
		if (mayInterruptOnCancel) {
			assertThat("Runnable should have been interrupted", runnable.wasInterrupted());
		} else {
			assertThat("Runnable should not have been interrupted", !runnable.wasInterrupted());
		}
	}
}
