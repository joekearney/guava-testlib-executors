package libjoe.testlib.executors.testers;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;

/**
 * Tests for cancellation of tasks submitted to an {@link ExecutorService}.
 * 
 * @author Joe Kearney
 * @see Future#cancel
 * @param <E> type of the executor under test
 */
public class CancellationTester<E extends ExecutorService> extends AbstractExecutorTester<E> {
	@Require(absent= {ExecutorFeature.SYNCHRONOUS_EXECUTE, ExecutorFeature.IGNORES_INTERRUPTS})
	public void testCancelRunning_submitRunnable_interrupt() throws Exception {
		doTestSubmitCancelRunning_interrupt(ExecutorSubmitters.RUNNABLE);
	}
    @Require(absent= {ExecutorFeature.SYNCHRONOUS_EXECUTE, ExecutorFeature.IGNORES_INTERRUPTS})
	public void testCancelRunning_submitRunnableWithValue_interrupt() throws Exception {
		doTestSubmitCancelRunning_interrupt(ExecutorSubmitters.RUNNABLE_WITH_VALUE);
	}
    @Require(absent= {ExecutorFeature.SYNCHRONOUS_EXECUTE, ExecutorFeature.IGNORES_INTERRUPTS})
	public void testCancelRunning_submitCallable_interrupt() throws Exception {
		doTestSubmitCancelRunning_interrupt(ExecutorSubmitters.CALLABLE);
	}
	private void doTestSubmitCancelRunning_interrupt(ExecutorSubmitter strategy) throws InterruptedException, BrokenBarrierException, TimeoutException, ExecutionException {
		RunnableWithBarrier task = new RunnableWithBarrier(2, 2);
		
		try {
			Future<?> future = strategy.submit(getSubjectGenerator().createTestSubject(), task);
			task.awaitBarrierDefault(); // task definitely running
			
			boolean cancelled = future.cancel(true);
			if (cancelled) {
				assertThat("Runnable should have been interrupted", task.wasInterrupted());
			}
			checkCancelledFuture(future);
		} finally {
			task.resetBarrier(); // just in case, keep things moving
		}
	}
	
	@Require(absent=ExecutorFeature.SYNCHRONOUS_EXECUTE)
	public void testCancelRunning_submitRunnable_noInterrupt() throws Exception {
		doTestSubmitCancelRunning_noInterrupt(ExecutorSubmitters.RUNNABLE);
	}
	@Require(absent=ExecutorFeature.SYNCHRONOUS_EXECUTE)
	public void testCancelRunning_submitRunnableWithValue_noInterrupt() throws Exception {
		doTestSubmitCancelRunning_noInterrupt(ExecutorSubmitters.RUNNABLE_WITH_VALUE);
	}
	@Require(absent=ExecutorFeature.SYNCHRONOUS_EXECUTE)
	public void testCancelRunning_submitCallable_noInterrupt() throws Exception {
		doTestSubmitCancelRunning_noInterrupt(ExecutorSubmitters.CALLABLE);
	}
	private void doTestSubmitCancelRunning_noInterrupt(ExecutorSubmitter submitter) throws InterruptedException, BrokenBarrierException, TimeoutException, ExecutionException {
		RunnableWithBarrier task = new RunnableWithBarrier(2, 3);
		
		try {
			Future<?> future = submitter.submit(getSubjectGenerator().createTestSubject(), task);
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
}
