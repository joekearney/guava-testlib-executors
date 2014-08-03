package libjoe.testlib.executors;

import java.util.concurrent.ExecutorService;

public abstract class ExecutorServiceTestSubjectGenerator<E extends ExecutorService> extends ExecutorTestSubjectGenerator<E> {
	void close(E usedExecutor) {
		if (usedExecutor != null) {
			usedExecutor.shutdownNow();
		}
	}
}
