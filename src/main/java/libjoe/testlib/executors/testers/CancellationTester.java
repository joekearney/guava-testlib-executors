package libjoe.testlib.executors.testers;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.LoggingRunnable;

import com.google.common.base.Throwables;

/**
 * Tests for cancellation of tasks submitted to an {@link ExecutorService}.
 *
 * @author Joe Kearney
 * @see Future#cancel
 * @param <E> type of the executor under test
 */
public class CancellationTester<E extends ExecutorService> extends AbstractOneSubmitterExecutorTester<E> {
	@Require(absent= {ExecutorFeature.SYNCHRONOUS_EXECUTION, ExecutorFeature.IGNORES_INTERRUPTS})
	public void testCancelRunning_Interrupt() throws Exception {
		RunnableWithBarrier task = new RunnableWithBarrier(2, 2);

		try {
			Future<?> future = submit(createExecutor(), task);
			task.awaitBarrierDefault(); // task definitely running

			boolean cancelled = future.cancel(true);
			if (cancelled) {
			    if (!task.wasInterrupted()) {
			        if (future.isDone()) {
			            try {
			                future.get();
			            } catch (Exception e) {
			                fail("Runnable should have been interrupted, but was not. " + Throwables.getStackTraceAsString(e));
			            }
			        }
			        fail("Runnable should have been interrupted, but was not");
			    }
			}
			checkCancelledFuture(future);
		} finally {
			task.resetBarrier(); // just in case, keep things moving
		}
	}

    @Require(absent=ExecutorFeature.SYNCHRONOUS_EXECUTION)
	public void testCancelRunning_NoInterrupt() throws Exception {
		RunnableWithBarrier task = new RunnableWithBarrier(2, 3);

		try {
			Future<?> future = submit(createExecutor(), task);
			task.awaitBarrierDefault(); // task now running
			boolean cancelled = future.cancel(false);
			assertThat("Should have been able to cancel task, but future.cancel(false) returned false", cancelled);
			task.awaitBarrierDefault(); // task now cancelled but still running

			assertThat("Future should be isCancelled() after cancellation even before completing", future.isCancelled());
			assertThat("Future should be isDone() after cancellation even before completing", future.isDone());
			assertThat("Runnable should not have been interrupted", !task.wasInterrupted());

			task.awaitBarrierDefault(); // task now done, just wait for future to be marked completed

			checkCancelledFuture(future);
			assertThat("Runnable should not have been interrupted", !task.wasInterrupted());
		} finally {
			task.resetBarrier(); // just in case
		}
	}

    public void testCancelCompletedFuture_MayInterrupt() throws Exception {
        doTestCancelCompletedFuture(true);
    }
    public void testCancelCompletedFuture_MayNotInterrupt() throws Exception {
        doTestCancelCompletedFuture(false);
    }
    private void doTestCancelCompletedFuture(boolean mayInterruptIfRunning) throws InterruptedException, ExecutionException,
            TimeoutException {
        LoggingRunnable task = noopRunnable();
        Future<?> future = submit(createExecutor(), task);
        checkCompletedFuture(task, future, getDefaultExpectedValue());
        assertFalse("Future#cancel(" + mayInterruptIfRunning + ") should not return true if invoked after completion", future.cancel(mayInterruptIfRunning));
        assertFalse("Task should not have been interrupted by Future#cancel(" + mayInterruptIfRunning + ") invoked after completion", task.wasInterrupted());
    }
}
