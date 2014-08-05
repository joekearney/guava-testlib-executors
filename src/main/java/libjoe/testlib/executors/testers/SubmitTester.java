package libjoe.testlib.executors.testers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;

@Require(value=ExecutorFeature.EXECUTOR_SERVICE)
public class SubmitTester<E extends ExecutorService> extends AbstractExecutorTester<E> {
	// TODO rejected execution
	
	public void testSubmitSingleTaskExecutes_Runnable() throws Exception {
		doTestSubmitSingleTaskExecutes(ExecutorSubmitters.RUNNABLE);
	}
	public void testSubmitSingleTaskExecutes_RunnableWithValue() throws Exception {
		doTestSubmitSingleTaskExecutes(ExecutorSubmitters.RUNNABLE_WITH_VALUE);
	}
	public void testSubmitSingleTaskExecutes_Callable() throws Exception {
		doTestSubmitSingleTaskExecutes(ExecutorSubmitters.CALLABLE);
	}
	public void testSubmitSingleTaskExecutes_InvokeAll() throws Exception {
	    doTestSubmitSingleTaskExecutes(ExecutorSubmitters.INVOKE_ALL);
	}
	private void doTestSubmitSingleTaskExecutes(ExecutorSubmitter strategy)
			throws InterruptedException, ExecutionException, TimeoutException {
		LoggingRunnable task = noopRunnable();
		Future<?> future = strategy.submit(getSubjectGenerator().createTestSubject(), task);
		checkCompletedFuture(task, future, strategy.getExpectedValue());
	}
	
	public void testSubmitSingleThrowingTaskExecutes_Runnable() throws Exception {
		doTestSubmitSingleThrowingTaskExecutes(ExecutorSubmitters.RUNNABLE);
	}
	public void testSubmitSingleThrowingTaskExecutes_RunnableWithValue() throws Exception {
		doTestSubmitSingleThrowingTaskExecutes(ExecutorSubmitters.RUNNABLE_WITH_VALUE);
	}
	public void testSubmitSingleThrowingTaskExecutes_Callable() throws Exception {
		doTestSubmitSingleThrowingTaskExecutes(ExecutorSubmitters.CALLABLE);
	}
	public void testSubmitSingleThrowingTaskExecutes_InvokeAll() throws Exception {
	    doTestSubmitSingleThrowingTaskExecutes(ExecutorSubmitters.INVOKE_ALL);
	}
	private void doTestSubmitSingleThrowingTaskExecutes(ExecutorSubmitter strategy) throws Exception {
		LoggingRunnable task = throwingRunnable();
		Future<?> future = strategy.submit(getSubjectGenerator().createTestSubject(), task);
		checkFutureAfterExecutionException(future);
		checkTaskRan(task);
	}
}
