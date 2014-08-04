package libjoe.testlib.executors.testers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CancellationTester<E extends ExecutorService> extends AbstractExecutorTester<E> {
	public void testSubmitCancelRunningRunnable_interrupt() throws Exception {
		RunnableWithBarrier runnable = newRunnableWithBarrier(2);
		try {
			Future<?> future = getSubjectGenerator().createTestSubject().submit(runnable);
			cancelFutureAndCheck(runnable, future, true);
		} finally {
			runnable.resetBarrier(); // just in case
		}
	}
}
