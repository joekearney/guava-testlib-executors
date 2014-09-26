package libjoe.testlib.executors.testers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.ExecutorSubmitter;
import libjoe.testlib.executors.LoggingRunnable;

import com.google.common.testing.NullPointerTester;

@Require(value=ExecutorFeature.EXECUTOR_SERVICE)
public class SubmitTester<E extends ExecutorService> extends AbstractOneSubmitterExecutorTester<E> {
	public void testSubmitSingleTaskExecutes() throws Exception {
		LoggingRunnable task = noopRunnable();
		ExecutorSubmitter<E> submitter = getSubjectGenerator().getSubmitter();
        Future<?> future = submitter.submit(createExecutor(), task);
		checkCompletedFuture(task, future, submitter.getExpectedValue());
	}

	public void testSubmitSingleThrowingTaskExecutes() throws Exception {
		LoggingRunnable task = throwingRunnable();
		Future<?> future = submit(createExecutor(), task);
		checkFutureAfterExecutionException(task, future);
		checkTaskRan(task);
	}

	public void testSubmitNullPointerExceptions() throws InterruptedException, NoSuchMethodException, SecurityException {
		E executor = createExecutor();
		new NullPointerTester().testMethod(executor, ExecutorService.class.getMethod("submit", Callable.class));
		new NullPointerTester().testMethod(executor, ExecutorService.class.getMethod("submit", Runnable.class));
		// don't test param index 1 for submit(Runnable, returnValue), because it's allowed to be null
		new NullPointerTester().testMethodParameter(executor, ExecutorService.class.getMethod("submit", Runnable.class), 0);
	}
}
