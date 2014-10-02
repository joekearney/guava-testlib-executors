package libjoe.testlib.executors;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.TesterAnnotation;
import com.google.common.util.concurrent.ListeningExecutorService;

// Enum values use constructors with generic varargs.
@GwtCompatible
public enum ExecutorFeature implements Feature<Executor> {
    /** Test subject is an instance of {@link Executor}. */
    EXECUTOR,
    /** Test subject is an instance of {@link ExecutorService}. */
    EXECUTOR_SERVICE(EXECUTOR),
    /** Test subject is an instance of {@link ListeningExecutorService}. */
    LISTENING(EXECUTOR_SERVICE),
    /** Test subject is an instance of {@link ScheduledExecutorService}. */
    SCHEDULED(EXECUTOR_SERVICE),

    /**
     * Indicates that a new task will be started before the caller returns. This is usually not compatible with
     * {@link #REJECTS_EXCESS_TASKS}. This happens in {@link Executors#newCachedThreadPool()}, for example, where a new thread is spun up
     * immediately.
     */
    SYNCHRONOUS_TASK_START,
    /**
     * Tasks are run synchronously with submission, so a call to execute() does not return before task completion, for example. This is true
     * for executors that run tasks in the calling thread.
     * <p>
     * Note that the {@link ExecutorService#invokeAll} runs effectively in this mode; see {@link ExecutorSubmitters#INVOKE_ALL}.
     *
     * @see ExecutorFeature#SYNCHRONOUS_EXECUTE_EXCEPTIONS
     */
    SYNCHRONOUS_EXECUTION,
    /**
     * Exceptions thrown from tasks submitted by {@link Executor#execute} are propagated out to the submitter. (The alternative would be to
     * swallow them.)
     * <p>
     * This could be because:<ul>
     * <li>the task was executed on the submitting thread, and exceptions are just thrown up the stack
     * <li>the task was executed elsewhere, but the submitting thread waited for completion, then rethrew any exceptions up the submitting stack
     * </ul>
     *
     * @see ExecutorFeature#SYNCHRONOUS_EXECUTE
     */
    SYNCHRONOUS_EXCEPTIONS,
    /**
     * Both {@link #SYNCHRONOUS_EXECUTE} (tasks are run synchronously with submission) and {@link #SYNCHRONOUS_EXECUTE_EXCEPTIONS}
     * (execution exceptions are thrown in the submitting thread).
     */
    SYNCHRONOUS(SYNCHRONOUS_EXECUTION, SYNCHRONOUS_EXCEPTIONS),

    /** When there are more tasks queued or running than can be handled, {@link RejectedExecutionException} is thrown on task submission. */
    REJECTS_EXCESS_TASKS,

    /** Does this executor ignore interrupts caused by cancellation? */
    IGNORES_INTERRUPTS,

    /**
     * Specify this feature if it's not possible to configure the executor with a {@link ThreadFactory} that builds the threads that
     * actually run the tasks. The important corrolary is that the test cannot record when interruption happens on a task thread.
     */
    NO_CONTROL_OF_THREAD_FACTORY(IGNORES_INTERRUPTS),
    /** Tasks are executed in the order in which they are submitted. */
    SERIALISED_EXECUTION,
    /** Shutdown methods are not supported */
    SHUTDOWN_SUPPRESSED,
    ;

    private final Set<Feature<? super Executor>> implied;

    @SafeVarargs
    private ExecutorFeature(Feature<? super Executor> ... implied) {
        this.implied = Helpers.copyToSet(implied);
    }

    @Override
    public Set<Feature<? super Executor>> getImpliedFeatures() {
        return implied;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @TesterAnnotation
    public @interface Require {
        public abstract ExecutorFeature[] value() default {};
        public abstract ExecutorFeature[] absent() default {};
    }
}