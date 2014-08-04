package libjoe.testlib.executors.testers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SubmitTester<E extends ExecutorService> extends AbstractExecutorTester<E> {
	// TODO rejected execution
	
	public void testSubmitSingleRunnableExecutes() throws Exception {
		LoggingRunnable task = noopRunnable();
		Future<?> future = getSubjectGenerator().createTestSubject().submit(task);
		checkFutureAfterCompleted(task, future, null);
	}
	public void testSubmitSingleThrowingRunnableExecutes() throws Exception {
		LoggingRunnable task = throwingRunnable();
		Future<?> future = getSubjectGenerator().createTestSubject().submit(task);
		checkFutureAfterExecutionException(future);
		checkTaskRan(task);
	}
	public void testSubmitSingleCallableExecutes() throws Exception {
		LoggingRunnable task = noopRunnable();
		Future<?> future = getSubjectGenerator().createTestSubject().submit(callableFor(task));
		checkFutureAfterCompleted(task, future, RETURN_VALUE);
	}
	public void testSubmitSingleThrowingCallableExecutes() throws Exception {
		LoggingRunnable task = throwingRunnable();
		Future<?> future = getSubjectGenerator().createTestSubject().submit(task);
		checkFutureAfterExecutionException(future);
		checkTaskRan(task);
	}
	public void testSubmitSingleRunnableAndValueExecutes() throws Exception {
		LoggingRunnable task = noopRunnable();
		Future<?> future = getSubjectGenerator().createTestSubject().submit(task, RETURN_VALUE);
		checkFutureAfterCompleted(task, future, RETURN_VALUE);
	}
	public void testSubmitSingleThrowingRunnableAndValueExecutes() throws Exception {
		LoggingRunnable task = throwingRunnable();
		Future<?> future = getSubjectGenerator().createTestSubject().submit(task, RETURN_VALUE);
		checkFutureAfterExecutionException(future);
		checkTaskRan(task);
	}
}
