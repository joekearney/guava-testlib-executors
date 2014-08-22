package libjoe.testlib.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Abstraction over the methods of an {@link ExecutorService} that submit tasks, like {@link ExecutorService#submit(Runnable)},
 * {@link ExecutorService#submit(Runnable, Object)}, {@link ExecutorService#submit(Callable)}.
 */
public interface ExecutorSubmitter {
    public static final Object RETURN_VALUE = new Object() {
        @Override
        public String toString() {
            return "DEFAULT_RETURN_VALUE";
        }
    };

    /**
     * Uses {@link Executor#execute(Runnable)}, returns a {@code null} {@link Future}.
     */
    public static final ExecutorSubmitter EXECUTE = new ExecutorSubmitter() {
        @Override
        public Future<?> submit(Executor executor, LoggingRunnable runnable) {
            executor.execute(runnable);
            return null;
        }
        @Override
        public Object getExpectedValue() {
            return null;
        }
    };

    Future<?> submit(Executor executor, LoggingRunnable runnable) throws InterruptedException;
    Object getExpectedValue();
}