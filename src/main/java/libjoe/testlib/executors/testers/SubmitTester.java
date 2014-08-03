package libjoe.testlib.executors.testers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SubmitTester<E extends ExecutorService> extends AbstractExecutorTester<E> {
	// TODO rejected execution
	
	public void testSubmitSingleRunnableExecutes() throws Exception {
		Future<?> future = getSubjectGenerator().createTestSubject().submit(noopRunnable());
		checkFutureAfterCompleted(future, null);
	}
	public void testSubmitSingleThrowingRunnableExecutes() throws Exception {
		Future<?> future = getSubjectGenerator().createTestSubject().submit(throwingRunnable());
		checkFutureAfterExecutionException(future);
	}
	public void testSubmitSingleCallableExecutes() throws Exception {
		Future<?> future = getSubjectGenerator().createTestSubject().submit(callableFor(noopRunnable()));
		checkFutureAfterCompleted(future, RETURN_VALUE);
	}
	public void testSubmitSingleThrowingCallableExecutes() throws Exception {
		Future<?> future = getSubjectGenerator().createTestSubject().submit(throwingRunnable());
		checkFutureAfterExecutionException(future);
	}
	public void testSubmitSingleRunnableAndValueExecutes() throws Exception {
		Future<?> future = getSubjectGenerator().createTestSubject().submit(noopRunnable(), RETURN_VALUE);
		checkFutureAfterCompleted(future, RETURN_VALUE);
	}
	public void testSubmitSingleThrowingRunnableAndValueExecutes() throws Exception {
		Future<?> future = getSubjectGenerator().createTestSubject().submit(throwingRunnable(), RETURN_VALUE);
		checkFutureAfterExecutionException(future);
	}
}
