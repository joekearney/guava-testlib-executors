package libjoe.testlib.executors.testers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.ExecutorSubmitter;
import libjoe.testlib.executors.LoggingRunnable;

@Require(value=ExecutorFeature.EXECUTOR_SERVICE)
public class SubmitTester<E extends ExecutorService> extends AbstractOneSubmitterExecutorTester<E> {
	public void testSubmitSingleTaskExecutes() throws Exception {
		LoggingRunnable task = noopRunnable();
		ExecutorSubmitter submitter = getSubjectGenerator().getSubmitter();
        Future<?> future = submitter.submit(getSubjectGenerator().createTestSubject(), task);
		checkCompletedFuture(task, future, submitter.getExpectedValue());
	}

	public void testSubmitSingleThrowingTaskExecutes() throws Exception {
		LoggingRunnable task = throwingRunnable();
		Future<?> future = submit(getSubjectGenerator().createTestSubject(), task);
		checkFutureAfterExecutionException(task, future);
		checkTaskRan(task);
	}
}
