package libjoe.testlib.executors;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.collect.Iterables;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.TesterAnnotation;

/**
 * {@link ExecutorSubmitter} implementations that return a future from the submission method.
 *
 * @author Joe Kearney
 */
public enum ExecutorServiceSubmitters implements ExecutorSubmitter<ExecutorService>, Feature<ExecutorService> {
    /**
     * Uses {@link ExecutorService#submit(Runnable)}, expects a {@code null} value in the {@link Future}.
     */
    RUNNABLE {
        @Override
        public Future<?> submit(ExecutorService executor, LoggingRunnable runnable) {
            return executor.submit(runnable);
        }
        @Override
        public Object getExpectedValue() {
            return null;
        }
        @Override
        public String getMethodString() {
        	return "submit(Runnable)";
        }
    },
    /**
     * Uses {@link ExecutorService#submit(Callable)}, expects {@link ExecutorSubmitter#RETURN_VALUE} as the value in the {@link Future}.
     */
    CALLABLE {
        @Override
        public Future<?> submit(ExecutorService executor, LoggingRunnable runnable) {
            return executor.submit(runnable.asCallableReturningDefault());
        }
        @Override
        public String getMethodString() {
        	return "submit(Callable)";
        }
    },
    /**
     * Uses {@link ExecutorService#submit(Runnable, Object)}, expects {@link ExecutorSubmitter#RETURN_VALUE} as the value in the
     * {@link Future}.
     */
    RUNNABLE_WITH_VALUE {
        @Override
        public Future<?> submit(ExecutorService executor, LoggingRunnable runnable) {
            return executor.submit(runnable, RETURN_VALUE);
        }
        @Override
        public String getMethodString() {
        	return "submit(Runnable, returnValue)";
        }
    },
    /**
     * Uses {@link ExecutorService#invokeAll(Collection)} with a collection holding only the parameter task, expects
     * {@link ExecutorSubmitter#RETURN_VALUE} as the value in the {@link Future}. Be aware that the task is specified to be executed
     * synchronously.
     */
    INVOKE_ALL(ExecutorFeature.SYNCHRONOUS_EXECUTE) {
        @Override
        public Future<?> submit(ExecutorService executor, LoggingRunnable runnable) throws InterruptedException {
            List<Callable<Object>> task = Arrays.asList(runnable.asCallableReturningDefault());
			List<Future<Object>> future = executor.invokeAll(task);
            return Iterables.getOnlyElement(future);
        }
        @Override
        public String getMethodString() {
        	return "invokeAll({oneTask})";
        }
    };

    @Override
    public Object getExpectedValue() {
        return RETURN_VALUE;
    }

    private final Set<Feature<? super ExecutorService>> implied;

    @SafeVarargs
    private ExecutorServiceSubmitters(Feature<? super ExecutorService>... implied) {
        this.implied = Helpers.copyToSet(implied);
    }

    @Override
    public Set<Feature<? super ExecutorService>> getImpliedFeatures() {
        return implied;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @TesterAnnotation
    public @interface Require {
        public abstract ExecutorServiceSubmitters[] value() default {};
        public abstract ExecutorServiceSubmitters[] absent() default {};
    }
}