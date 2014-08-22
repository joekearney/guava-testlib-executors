package libjoe.testlib.executors.testers;

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

@Require(value = ExecutorFeature.EXECUTOR_SERVICE)
public class InvokeAllTester<E extends ExecutorService> extends AbstractExecutorTester<E, ExecutorTestSubjectGenerator<E>> {
	private static final int NUM_TASKS_TO_INVOKE = 8;
	private int getNumberOfTasksToExecute() {
		int maxTasks = getSubjectGenerator().getMaxQueuedCapacity();
		if (maxTasks != ExecutorTestSubjectGenerator.UNASSIGNED) {
			return maxTasks;
		} else {
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
		List<Future<Object>> futures = createExecutor().invokeAll(asCallables(runnables), getTimeoutDuration(),
				getTimeoutUnit());
		assertThat(futures, is(Matchers.<Future<Object>>iterableWithSize(runnables.size())));
		for (int i = 0; i < runnables.size(); i++) {
			checkCompletedFuture(runnables.get(i), futures.get(i), ExecutorSubmitter.RETURN_VALUE);
		}
	}
	public void testInvokeAllMixedCompletesAllTasks_NoTimeout() throws Exception {
		List<LoggingRunnable> runnables = Arrays.<LoggingRunnable>asList(noopRunnable(), throwingRunnable(), noopRunnable());
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
	
	public void testInvokeAllNullPointerExceptions() throws NoSuchMethodException, SecurityException {
		runNullPointerTests("invokeAll");
	}
}
