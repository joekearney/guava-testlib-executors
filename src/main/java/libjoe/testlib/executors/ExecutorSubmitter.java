package libjoe.testlib.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Abstraction over the methods of an {@link ExecutorService} that submit tasks, like {@link ExecutorService#submit(Runnable)},
 * {@link ExecutorService#submit(Runnable, Object)}, {@link ExecutorService#submit(Callable)}.
 *
 * @see ExecutorServiceSubmitters
 */
public interface ExecutorSubmitter<E extends Executor> {
    public static final Object RETURN_VALUE = new Object() {
        @Override
        public String toString() {
            return "DEFAULT_RETURN_VALUE";
        }
    };

    /**
     * Uses {@link Executor#execute(Runnable)}, returns a {@code null} {@link Future}.
     */
    public static final ExecutorSubmitter<Executor> EXECUTE = new ExecutorSubmitter<Executor>() {
        @Override
        public Future<?> submit(Executor executor, LoggingRunnable runnable) {
            executor.execute(runnable);
            return null;
        }
        @Override
        public Object getExpectedValue() {
            return null;
        }
        @Override
        public String getMethodString() {
        	return "execute(Runnable)";
        }
    };

    Future<?> submit(E executor, LoggingRunnable runnable) throws InterruptedException;
    Object getExpectedValue();
	String getMethodString();
}