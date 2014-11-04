package libjoe.testlib.executors.testers;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.ExecutorSubmitter;
import libjoe.testlib.executors.ExecutorTestSubjectGenerator;
import libjoe.testlib.executors.LoggingRunnable;

import org.hamcrest.Matchers;

import com.google.common.collect.ImmutableList;

@Require(value = ExecutorFeature.EXECUTOR_SERVICE)
public class InvokeAllTester<E extends ExecutorService> extends AbstractExecutorTester<E, ExecutorTestSubjectGenerator<E>> {
	private static final int NUM_TASKS_TO_INVOKE = 8;
	private int getNumberOfTasksToExecute() {
		int maxTasks = getSubjectGenerator().getMaxQueuedCapacity();
		if (maxTasks != ExecutorTestSubjectGenerator.UNASSIGNED) {
			return maxTasks;
		} else {
		    // finger in the air
			return NUM_TASKS_TO_INVOKE;
		}
	}

	public void testInvokeAllCompletesAllTasks_NoTimeout() throws Exception {
		List<LoggingRunnable> runnables = createManyNoopRunnables(getNumberOfTasksToExecute());
		List<Future<Object>> futures = createExecutor().invokeAll(asCallables(runnables));
		assertThat(futures, is(Matchers.<Future<Object>>iterableWithSize(runnables.size())));
		for (int i = 0; i < runnables.size(); i++) {
			checkCompletedFuture(runnables.get(i), futures.get(i), ExecutorSubmitter.RETURN_VALUE);
		}
	}
	public void testInvokeAllCompletesAllTasks_LongTimeout() throws Exception {
		List<LoggingRunnable> runnables = createManyNoopRunnables(getNumberOfTasksToExecute());
		List<Future<Object>> futures = createExecutor().invokeAll(asCallables(runnables), getTimeoutDuration(), getTimeoutUnit());
		assertThat(futures, is(Matchers.<Future<Object>>iterableWithSize(runnables.size())));
		for (int i = 0; i < runnables.size(); i++) {
			checkCompletedFuture(runnables.get(i), futures.get(i), ExecutorSubmitter.RETURN_VALUE);
		}
	}
    public void testInvokeAllMixedCompletesAllTasks_NoTimeout() throws Exception {
        /*
         * This test fails sporadically for ForkJoinPool, possibly more consistently with parallelism=2 than 3. ForkJoinPool#invokeAll
         * cancels tasks when it sees an exception. Whether this cancellation makes it into the returned future depends on a race condition
         * in (parallel) execution of the tasks.
         *
         * It's not completely clear whether this complies with the spec. The spec doesn't explicitly state that the tasks run
         * independently, but it feels odd for behaviour of later tasks to depend on earlier ones that threw an exception. That said,
         * perhaps fork-join should expect this sort of coupling between tasks, in which case cancellation of subsequent tasks may be
         * reasonable.
         */

        List<LoggingRunnable> runnables = Arrays.<LoggingRunnable> asList(noopRunnable(), throwingRunnable(), noopRunnable());
        List<Future<Object>> futures = createExecutor().invokeAll(asCallables(runnables));
        checkCompletedFuture(runnables.get(0), futures.get(0), ExecutorSubmitter.RETURN_VALUE);
        checkFutureAfterExecutionException(runnables.get(1), futures.get(1));
        checkCompletedFuture(runnables.get(2), futures.get(2), ExecutorSubmitter.RETURN_VALUE);
    }
	public void testInvokeAllMixedCompletesAllTasks_LongTimeout() throws Exception {
		List<LoggingRunnable> runnables = Arrays.<LoggingRunnable>asList(noopRunnable(), throwingRunnable(), noopRunnable());
		List<Future<Object>> futures = createExecutor().invokeAll(asCallables(runnables), getTimeoutDuration(), getTimeoutUnit());
		checkCompletedFuture(runnables.get(0), futures.get(0), ExecutorSubmitter.RETURN_VALUE);
		checkFutureAfterExecutionException(runnables.get(1), futures.get(1));
		checkCompletedFuture(runnables.get(2), futures.get(2), ExecutorSubmitter.RETURN_VALUE);
	}

	@Require(value = ExecutorFeature.SERIALISED_EXECUTION, absent = ExecutorFeature.SYNCHRONOUS_EXCEPTIONS)
	public void testInvokeAllMixedCompletesAllTasks_ShortTimeout_Async() throws Exception {
		List<LoggingRunnable> runnables = Arrays.<LoggingRunnable> asList(noopRunnable(), new RunnableWithBarrier(2, 1), noopRunnable());
		List<Future<Integer>> futures = createExecutor().invokeAll(asCountingCallables(runnables), 20,
				TimeUnit.MILLISECONDS);
		checkCompletedFuture(runnables.get(0), futures.get(0), 0);
		checkCancelledFuture(futures.get(1));
		checkCancelledFuture(futures.get(2));
	}
	@Require(value = { ExecutorFeature.SERIALISED_EXECUTION, ExecutorFeature.SYNCHRONOUS_EXCEPTIONS })
	public void testInvokeAllMixedCompletesAllTasks_ShortTimeout_Sync() throws Exception {
		List<LoggingRunnable> runnables = Arrays.<LoggingRunnable>asList(noopRunnable(), new RunnableWithBarrier(2, 1), noopRunnable());
		List<Future<Integer>> futures = createExecutor().invokeAll(asCountingCallables(runnables), 20, TimeUnit.MILLISECONDS);
		checkCompletedFuture(runnables.get(0), futures.get(0), 0);
		checkFutureAfterExecutionException(runnables.get(1), futures.get(1), RuntimeException.class, TimeoutException.class);
		checkCancelledFuture(futures.get(2));
	}

	private Collection<Callable<Integer>> asCountingCallables(List<? extends LoggingRunnable> runnables) {
		List<Callable<Integer>> callables = new ArrayList<>(runnables.size());
		for (int i = 0; i < runnables.size(); i++) {
			LoggingRunnable runnable = runnables.get(i);
			callables.add(runnable.asCallableReturningValue(i));
		}
		return callables;
	}

	public void testInvokeAllEmpty_NoTimeout() throws InterruptedException {
		List<Future<Object>> futures = createExecutor().invokeAll(Collections.<Callable<Object>> emptyList());
		assertThat("ExecutorServices#invokeAll(empty) should return an empty list", futures, is(Collections.<Future<Object>> emptyList()));
	}
	public void testInvokeAllEmpty_LongTimeout() throws InterruptedException {
		List<Future<Object>> futures = createExecutor().invokeAll(Collections.<Callable<Object>> emptyList(), getTimeoutDuration(), getTimeoutUnit());
		assertThat("ExecutorServices#invokeAll(empty) should return an empty list", futures, is(Collections.<Future<Object>> emptyList()));
	}
	public void testInvokeAllSingletonNull_NoTimeout() throws InterruptedException {
		try {
			List<Future<Object>> futures = createExecutor().invokeAll(Arrays.<Callable<Object>>asList((Callable<Object>)null));
			fail("invokeAll({null}) should have thrown NullPointerException, but it returned " + futures);
		} catch (NullPointerException e) {
			//expected
		}
	}
	public void testInvokeAllSingletonNull_LongTimeout() throws InterruptedException {
		try {
			List<Future<Object>> futures = createExecutor().invokeAll(Arrays.<Callable<Object>>asList((Callable<Object>)null), getTimeoutDuration(), getTimeoutUnit());
			fail("invokeAll({null}) should have thrown NullPointerException, but it returned " + futures);
		} catch (NullPointerException e) {
			//expected
		}
	}
	public void testInvokeAllNullAfterCallable_NoTimeout() throws InterruptedException {
		try {
			List<Future<Object>> futures = createExecutor().invokeAll(Arrays.<Callable<Object>>asList(noopRunnable().asCallableReturningDefault(), null));
			fail("invokeAll({callable, null}) should have thrown NullPointerException, but it returned " + futures);
		} catch (NullPointerException e) {
			//expected
		}
	}
	public void testInvokeAllNullAfterCallable_LongTimeout() throws InterruptedException {
		try {
			List<Future<Object>> futures = createExecutor().invokeAll(Arrays.<Callable<Object>>asList(noopRunnable().asCallableReturningDefault(), null), getTimeoutDuration(), getTimeoutUnit());
			fail("invokeAll({callable, null}) should have thrown NullPointerException, but it returned " + futures);
		} catch (NullPointerException e) {
			//expected
		}
	}
	public void testInvokeAllNull_NoTimeout() throws InterruptedException {
		try {
			List<Future<Object>> futures = createExecutor().invokeAll(null);
			fail("invokeAll(null) should have thrown NullPointerException, but it returned " + futures);
		} catch (NullPointerException e) {
			//expected
		}
	}
	public void testInvokeAllNull_LongTimeout() throws InterruptedException {
		try {
			List<Future<Object>> futures = createExecutor().invokeAll(null, getTimeoutDuration(), getTimeoutUnit());
			fail("invokeAll(null) should have thrown NullPointerException, but it returned " + futures);
		} catch (NullPointerException e) {
			//expected
		}
	}

    // Timeout tests
    @Require(absent=ExecutorFeature.SYNCHRONOUS_EXCEPTIONS)
    public void testLongRunningOneTaskTimesOut() throws Exception {
        doTestLongRunningTasksTimesOut(1);
    }
    @Require(absent=ExecutorFeature.SYNCHRONOUS_EXCEPTIONS)
    public void testLongRunningManyTasksTimesOut() throws Exception {
        doTestLongRunningTasksTimesOut(Math.max(2, getNumberOfTasksToExecute()));
    }
    @Require(value=ExecutorFeature.SYNCHRONOUS_EXCEPTIONS)
    public void testLongRunningOneTaskTimesOut_SyncExceptions() throws Exception {
        try {
            doTestLongRunningTasksTimesOut(1);
        } catch (Exception e) {
            assertRootCause(e, TimeoutException.class);
        }
    }
    @Require(value=ExecutorFeature.SYNCHRONOUS_EXCEPTIONS)
    public void testLongRunningManyTasksTimesOut_SyncExceptions() throws Exception {
        try {
            doTestLongRunningTasksTimesOut(Math.max(2, getNumberOfTasksToExecute()));
        } catch (Exception e) {
            assertRootCause(e, TimeoutException.class);
        }
    }
    private void doTestLongRunningTasksTimesOut(int count) throws Exception {
        /*
         * Synchronous versions allow the interpretation of the spec where the caught InterruptionException is the failure that causes an
         * ExecutionException to be thrown.
         */
        ImmutableList.Builder<Callable<Object>> builder = ImmutableList.builder();
        for (int i = 0; i < count; i++) {
            builder.add(new RunnableWithBarrier(2, 1).asCallableReturningDefault());
        }
        List<Future<Object>> results = createExecutor().invokeAll(builder.build(), 10, MILLISECONDS);
        for (Future<Object> result : results) {
            checkCancelledFuture(result);
        }
    }

    // Interruption tests
    /*
     * Don't respect ExecutorFeature.IGNORES_INTERRUPTS here, which applies only to the threads created by the test thread factory.
     * Synchronous versions allow the interpretation of the spec where the caught InterruptionException is the failure that causes
     * an ExecutionException to be thrown.
     */
    public void testInterruptedWhileWaiting_NoTimeout() throws Exception {
        doTestInterruptedWhileWaiting(false);
    }
    public void testInterruptedWhileWaiting_Timeout() throws Exception {
        doTestInterruptedWhileWaiting(true);
    }
    private void doTestInterruptedWhileWaiting(boolean withTimeout) throws TimeoutException {
        final RunnableWithBarrier task1 = new RunnableWithBarrier(2, 2);
        final RunnableWithBarrier task2 = new RunnableWithBarrier(2, 2);
        ImmutableList<Callable<Object>> tasks = ImmutableList.of(task1.asCallableReturningDefault(), task2.asCallableReturningDefault());

        interruptMeAtBarrier(task1);
        try {
            final List<Future<Object>> results;
            if (withTimeout) {
                results = createExecutor().invokeAll(tasks, getTimeoutDuration(), getTimeoutUnit());
            } else {
                results = createExecutor().invokeAll(tasks);
            }

            /*
             * TODO Guava MoreExecutors.newDirectExecutorService captures the InterruptedException in an ExecutionException -
             * it records it in the task not in the call to invokeAll. This is incorrect according to the spec of invokeAll,
             * but probably not reasonable to expect in a direct implementation.
             */
//            try {
//                results.get(0).get(getTimeoutDuration(), getTimeoutUnit());
//            } catch (Throwable e) {
//                assertThat("Expected an InterruptedException in the causal chain, but found none",
//                        any(getCausalChain(e), instanceOf(InterruptedException.class)));
//            }

            fail("Interrupted while waiting on invokeAny(task), should have thrown InterruptedException or an exception with that root cause");
        } catch (InterruptedException expected) {}
    }

	public void testInvokeAllNullPointerExceptions() throws NoSuchMethodException, SecurityException {
		runNullPointerTests("invokeAll");
	}
}
