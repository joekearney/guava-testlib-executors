package libjoe.testlib.executors;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.TesterAnnotation;

/**
 * {@link ExecutorSubmitter} implementations that return a future from the submission method.
 *
 * @author Joe Kearney
 */
public enum ScheduledExecutorServiceSubmitters implements ExecutorSubmitter<ScheduledExecutorService>, Feature<ScheduledExecutorService> {
    /**
     * Uses {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)}, expects a {@code null} value in the {@link ScheduledFuture}.
     */
    RUNNABLE_ZERO_DELAY {
        @Override
        public ScheduledFuture<?> submit(ScheduledExecutorService executor, LoggingRunnable runnable) {
            return executor.schedule(runnable, 0, TimeUnit.MILLISECONDS);
        }
        @Override
        public Object getExpectedValue() {
            return null;
        }
        @Override
        public String getMethodString() {
            return "schedule(Runnable, 0)";
        }
    },
    /**
     * Uses {@link ScheduledExecutorService#schedule(Callable, long, TimeUnit)}, expects {@link ExecutorSubmitter#RETURN_VALUE} as the value
     * in the {@link ScheduledFuture}.
     */
    CALLABLE_ZERO_DELAY {
        @Override
        public Future<?> submit(ScheduledExecutorService executor, LoggingRunnable runnable) {
            return executor.schedule(runnable.asCallableReturningDefault(), 0, TimeUnit.MILLISECONDS);
        }
        @Override
        public String getMethodString() {
            return "schedule(Callable, 0)";
        }
    },
    ;

    @Override
    public Object getExpectedValue() {
        return RETURN_VALUE;
    }

    private final Set<Feature<? super ScheduledExecutorService>> implied;

    @SafeVarargs
    private ScheduledExecutorServiceSubmitters(Feature<? super ScheduledExecutorService>... implied) {
        this.implied = Helpers.copyToSet(implied);
    }

    @Override
    public Set<Feature<? super ScheduledExecutorService>> getImpliedFeatures() {
        return implied;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @TesterAnnotation
    public @interface Require {
        public abstract ScheduledExecutorServiceSubmitters[] value() default {};
        public abstract ScheduledExecutorServiceSubmitters[] absent() default {};
    }
}