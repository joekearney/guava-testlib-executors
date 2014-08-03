package libjoe.testlib.executors.testers;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class ExecuteTester<E extends Executor> extends AbstractExecutorTester<E> {
	public void testExecuteSingleTaskExecutes() throws Exception {
		RunnableWithLatch runnableWithLatch = newRunnableWithLatch();
		getSubjectGenerator().createTestSubject().execute(runnableWithLatch);
		runnableWithLatch.await(1, TimeUnit.SECONDS);
	}
}
