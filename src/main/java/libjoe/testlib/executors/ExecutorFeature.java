package libjoe.testlib.executors;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
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
	
	/** Tasks are run synchronously with submission, so a call to execute() does not return before task completion, for example. */
	SYNCHRONOUS_EXECUTE,
    /**
     * Exceptions thrown from tasks submitted by {@link Executor#execute} are propagated out to the submitter. (The alternative would be to
     * swallow them.)
     * <p>
     * This could be because:<ul>
     * <li>the task was executed on the submitting thread, and exceptions are just thrown up the stack
     * <li>the task was executed elsewhere, but the submitting thread waited for completion, then rethrew any exceptions up the submitting stack
     * </ul>
     */
	SYNCHRONOUS_EXECUTE_EXCEPTIONS,
	/** Both {@link #SYNCHRONOUS_EXECUTE} and {@link #SYNCHRONOUS_EXECUTE_EXCEPTIONS}. */
	SYNCHRONOUS(SYNCHRONOUS_EXECUTE, SYNCHRONOUS_EXECUTE_EXCEPTIONS),
	
	/** When there are more tasks queued or running than can be handled, {@link RejectedExecutionException} is thrown on task submission. */
	REJECTS_EXCESS_TASKS,
	
	/** Does this executor ignore interrupts caused by cancellation? */
	IGNORES_INTERRUPTS,
	
    /**
     * Specify this feature if it's not possible to configure the executor with a {@link ThreadFactory} that builds the threads that
     * actually run the tasks.
     */
	NO_CONTROL_OF_THREAD_FACTORY(IGNORES_INTERRUPTS),
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