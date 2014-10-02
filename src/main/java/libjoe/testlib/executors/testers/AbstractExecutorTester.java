package libjoe.testlib.executors.testers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.io.Closeable;
import java.lang.Thread.State;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.AssertionFailedError;
import libjoe.testlib.executors.ExecutorSubmitter;
import libjoe.testlib.executors.ExecutorTestSubjectGenerator;
import libjoe.testlib.executors.LoggingRunnable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.testing.NullPointerTester;

/**
 * Support for testers for {@link Executor}s.
 *
 * @author Joe Kearney
 * @param <E> type of the {@link Executor}
 */
public abstract class AbstractExecutorTester<E extends Executor, G extends ExecutorTestSubjectGenerator<E>> extends AbstractTester<G> {
    private final Queue<Closeable> toClose = new ConcurrentLinkedQueue<>();

    @Override
    public final void setUp() throws Exception {
        getSubjectGenerator().setTester(this);
    }
    @Override
    public final void tearDown() throws Exception {
        while (!toClose.isEmpty()) {
            try {
                toClose.poll().close();
            } catch (Exception swallow) {}
        }
        getSubjectGenerator().tearDown();

        // clear interrupted flag
        Thread.interrupted();
    }

    /**
     * Creates the test executor.
     *
     * @return a new test subject
     */
	protected final E createExecutor() {
		return getSubjectGenerator().createTestSubject();
	}

    /**
     * Fails the test with a causal exception.
     *
     * @param message failure message
     * @param cause exception that caused the failure
     */
    public static void fail(String message, Throwable cause) {
        AssertionFailedError afe = new AssertionFailedError(message);
        afe.initCause(cause);
        throw afe;
    }

    protected final void addTasksToCapacity(E executor, ExecutorSubmitter<? super E> submitter) throws InterruptedException, BrokenBarrierException, TimeoutException {
        int maxCapacity = getSubjectGenerator().getMaxCapacity();
        int concurrencyLevel = getSubjectGenerator().getConcurrencyLevel();

        List<RunnableWithBarrier> runningTasks = createManyRunnablesWithBarriers(concurrencyLevel, 2, 2);
        List<RunnableWithBarrier> queuedTasks = createManyRunnablesWithBarriers(maxCapacity - concurrencyLevel, 2, 1);

        for (RunnableWithBarrier task : runningTasks) {
            submitter.submit(executor, task);
            task.awaitBarrierDefault();
            checkTaskIsBlocked(task);
        }
        // now running as many tasks as possible, none queued yet

        // now fill the queue
        for (LoggingRunnable task : queuedTasks) {
            submitter.submit(executor, task);
        }

        for (RunnableWithBarrier queuedTask : queuedTasks) {
            assertThat("Task should be not have run yet, since it's supposed to be waiting in the queue", !queuedTask.wasRun());
        }

        for (RunnableWithBarrier runningTask : runningTasks) {
            assertThat("Task should be running now, since we waited for the first barrier", runningTask.wasRun());
            assertThat("Task should not have finished yet, it should be waiting on the second barrier", !runningTask.hasFinished());
            checkTaskIsBlocked(runningTask); // check still blocked
        }
    }

    @SafeVarargs
    protected final <T> void assertThrows(Future<T> future, Class<? extends Throwable> ... types) {
        try {
            future.get(getTimeoutDuration(), getTimeoutUnit());
            fail("Should have thrown an exception, but nothing was thrown");
        } catch (ExecutionException e) {
            // pass if it's the right type
            Throwable cause = e.getCause();
            for (int i = 0; i < types.length; i++) {
                Class<? extends Throwable> expectedType = types[i];
                assertThat("Expected exception of type " + expectedType.getName() + " at position " + i
                        + " in the causal chain, but there was no such causal exception", cause, is(notNullValue()));
                assertThat("Wrong type of cause exception for the ExecutionException", cause, instanceOf(expectedType));
                cause = cause.getCause();
            }
        } catch (Throwable e) {
            AssertionFailedError afe = new AssertionFailedError("Threw wrong type of exception. Expected an ExecutionException.");
            afe.initCause(e);
            throw afe;
        }
    }
    protected final void checkTaskRan(LoggingRunnable runnable) throws TimeoutException {
        try {
            runnable.awaitRunningDefault();
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted while waiting for task to finish", e);
        }
        assertThat("Task should have been executed, but it didn't run.", runnable.wasRun());
    }
    protected final void checkCompletedFuture(LoggingRunnable task, Future<?> future, Object expected) throws InterruptedException,
            ExecutionException, TimeoutException {
        Object result = future.get(getTimeoutDuration(), getTimeoutUnit());
        checkTaskRan(task);
        assertThat("Future should be isDone() after get() returns", future.isDone());
        assertThat("Future should not be isCancelled() after get() returns", !future.isCancelled());
        assertThat("Future should return expected value", result, is(sameInstance(expected)));
    }
    protected final void checkFutureAfterExecutionException(LoggingRunnable task, Future<?> future) throws TimeoutException {
        checkFutureAfterExecutionException(task, future, RuntimeRunnableException.class);
    }
    @SafeVarargs
    protected final void checkFutureAfterExecutionException(LoggingRunnable task, Future<?> future, Class<? extends Throwable> ... exceptionClasses) throws TimeoutException {
        assertThrows(future, exceptionClasses);
        checkTaskRan(task);
        assertThat("Future should be isDone() after get() throws", future.isDone());
        assertThat("Future should not be isCancelled() after get() throws", !future.isCancelled());
    }
    protected final void checkCancelledFuture(Future<?> future) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            Object value = future.get(getTimeoutDuration(), getTimeoutUnit());
            fail("Cancelled task should have thrown cancellation exception from get(), but it returned value " + value);
        } catch (CancellationException e) {
            // pass
        }
        assertThat("Future should be isCancelled() after cancellation", future.isCancelled());
        assertThat("Future should be isDone() after cancellation and completion", future.isDone());
    }
    private static final Set<Thread.State> BLOCKING_STATES = Sets.immutableEnumSet(State.BLOCKED, State.WAITING, State.TIMED_WAITING);
    protected final void checkTaskIsBlocked(LoggingRunnable runningTask) {
        /*
         * ... or is blocked soon. Common pattern is to have two barriers in the Runnable, and await once in the main
         * thread. Once that first barrier has been passed the Runnable is definitely running, and it'll be blocking
         * on the barrier in a moment.
         */

        Thread thread = runningTask.getRunningThread();
        assertThat("Task was expected to be in blocked state, but it hasn't started yet. Task: " + runningTask, thread != null);
        State state = thread.getState();
        int tries = 0;
        while (tries++ < 5 && !BLOCKING_STATES.contains(state)) {
            // give it just a little pause to see if we need to wait
            trySleep(10);
            state = thread.getState();
        }
        assertThat("Expected task in a blocked state (one of " + BLOCKING_STATES + ") but it was " + state, BLOCKING_STATES.contains(state));
    }

    private static void trySleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected final void awaitTermination(ExecutorService executorService) throws InterruptedException, TimeoutException {
        if (!executorService.awaitTermination(getTimeoutDuration(), getTimeoutUnit())) {
            throw new TimeoutException("Executor did not terminate quickly enough");
        }
        assertTrue("ExecutorService should be isTerminated() after awaitTermination returns true", executorService.isTerminated());
    }

    protected final long getTimeoutDuration() {
        // TODO make this dependent on features of the test subject? longer for "really" async stuff, if required
        return 150;
    }
    protected final TimeUnit getTimeoutUnit() {
        return TimeUnit.MILLISECONDS;
    }

    protected abstract class AbstractLoggingRunnable implements LoggingRunnable {
        private final CountDownLatch latchForFirstRun = new CountDownLatch(1);
        private volatile boolean ran = false;
        private volatile boolean finished = false;
        private volatile Thread runningThread = null;

        @Override
        public final synchronized void run() {
            ran = true;
            try {
                try {
                    Thread thisThread = Thread.currentThread();
                    checkState(runningThread == null, "Runnable should be executed only once. Already executed on thread[%s], tried again on thread [%s]", runningThread, thisThread.getName());
                    runningThread = thisThread;
                    // logRunning(thisThread);
                } finally {
                    latchForFirstRun.countDown();
                }
                doRun();
            } finally {
                finished = true;
            }
        }

        @Override
        public final Callable<Object> asCallableReturningDefault() {
            return Executors.callable(this, ExecutorSubmitter.RETURN_VALUE);
        }
        @Override
        public final Callable<Object> asCallableReturningNothing() {
            return Executors.callable(this);
        }
        @Override
        public <T> Callable<T> asCallableReturningValue(T result) {
            return Executors.callable(this, result);
        }

        @Override
        public final boolean wasInterrupted() {
            return getSubjectGenerator().wasInterrupted(runningThread);
        }
        @Override
        public final boolean wasRun() {
            return ran;
        }
        @Override
        public final boolean hasFinished() {
            return finished;
        }
        @Override
        public final Thread getRunningThread() {
            return runningThread;
        }

        abstract void doRun();

        @Override
        public void awaitRunningDefault() throws InterruptedException, TimeoutException {
            if (!latchForFirstRun.await(getTimeoutDuration(), getTimeoutUnit())) {
                throw new TimeoutException();
            }
        }

        @SuppressWarnings("unused")
        private void logRunning(Thread thisThread) {
            String className = getClass().getSimpleName();
            if (className.isEmpty()) {
                className = getClass().getName();
                int lastDot = className.lastIndexOf('.');
                if (lastDot >= 0 && className.length() > lastDot) {
                    className = className.substring(lastDot + 1);
                }
                String superClassName = getClass().getSuperclass().getSimpleName();
                className = className + " extends " + superClassName + " in " + getTestMethodName();
            }
            System.out.println("Running [" + className + "] in thread[" + thisThread.getName() + "]");
        }
    }
    protected final List<RunnableWithBarrier> createManyRunnablesWithBarriers(int numToCreate, int parties, int rounds) {
        checkArgument(numToCreate >= 0, "Cannot create [%s] barriers", numToCreate);
        List<RunnableWithBarrier> tasks = new ArrayList<>(numToCreate);
        for (int i = 0; i < numToCreate; i++) {
            tasks.add(new RunnableWithBarrier(parties, rounds));
        }
        return tasks;
    }
    /**
     * From another thread in a new executor, awaits on the barrier in the task, then interrupts the calling thread.
     */
    protected final void interruptMeAtBarrier(final RunnableWithBarrier task) {
        final Thread mainThread = Thread.currentThread();
        final ExecutorService ancilliary = newAncilliarySingleThreadedExecutor();
        ancilliary.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                task.awaitBarrierDefault();
                mainThread.interrupt();
                ancilliary.shutdown();
                return null;
            }
        });
    }
    protected class RunnableWithBarrier extends AbstractLoggingRunnable {
        private final CyclicBarrier barrier;
        private final int rounds;
        private final AtomicInteger roundsCompleted = new AtomicInteger();

        protected RunnableWithBarrier(int parties, int rounds) {
            barrier = new CyclicBarrier(parties);
            checkArgument(rounds > 0, "Cannot create a RunnableWithBarrier that never waits on any rounds. rounds=[%s] but should be > 0");
            this.rounds = rounds;
        }

        public final void awaitBarrier(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
            barrier.await(timeout, unit);
        }
        public final void awaitBarrierDefault() throws InterruptedException, BrokenBarrierException, TimeoutException {
            barrier.await(getTimeoutDuration(), getTimeoutUnit());
        }
        public final void resetBarrier() {
            barrier.reset();
        }
        public final int getRoundsCompleted() {
            return roundsCompleted.get();
        }

        protected final void doAwait() {
            try {
                barrier.await(getTimeoutDuration(), getTimeoutUnit());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeInterruptedException(e);
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new RuntimeException(e);
            } finally {
                roundsCompleted.incrementAndGet();
            }
        }

        @Override
        protected void doRun() {
            for (int i = 0; i < rounds; i++) {
                doAwait();
            }
        }
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("roundsCompleted", roundsCompleted).add("of", rounds).toString();
        }
    }
    /** Creates a {@link Runnable} that does nothing. */
    protected final LoggingRunnable noopRunnable() {
        return new NoopRunnable();
    }
    protected final List<LoggingRunnable> createManyNoopRunnables(int numToCreate) {
        List<LoggingRunnable> tasks = new ArrayList<LoggingRunnable>(numToCreate);
        for (int i = 0; i < numToCreate; i++) {
            tasks.add(noopRunnable());
        }
        return tasks;
    }
    private final class NoopRunnable extends AbstractLoggingRunnable {
        @Override
        void doRun() {}
        @Override
        public String toString() {
        	return getClass().getSimpleName();
        }
    }
    /** Creates a {@link Runnable} that throws {@link RuntimeRunnableException}. */
    protected final LoggingRunnable throwingRunnable() {
        return new ThrowingRunnable();
    }
    protected final void runNullPointerTests(String methodName) {
		E executor = createExecutor();
		boolean ranTests = false;
		for (Method method : ExecutorService.class.getMethods()) {
			if (method.getName().equals(methodName)) {
				ranTests = true;
				new NullPointerTester().testMethod(executor, method);
			}
		}
		if (!ranTests) {
			throw new AssertionError("Found no methods called [" + methodName + "] on which to run NullPointerTester tests");
		}
	}
	private final class ThrowingRunnable extends AbstractLoggingRunnable {
        @Override
        void doRun() throws RuntimeRunnableException {
            throw new RuntimeRunnableException();
        }
        @Override
        public String toString() {
        	return getClass().getSimpleName();
        }
    }
    protected final class UninterruptibleRunnable extends AbstractLoggingRunnable implements Closeable {
        private volatile boolean stopped = false;

        public UninterruptibleRunnable() {
            registerToClose(this);
        }

        @Override
        void doRun() {
            while (!stopped) {}
        }
        @Override
        public void close() {
            stopped = true;
        }
    }
    /**
     * Exception thrown by a {@link AbstractExecutorTester#throwingRunnable()}.
     */
    protected static final class RuntimeRunnableException extends RuntimeException {
        private static final long serialVersionUID = 8792817003755375674L;
        public RuntimeRunnableException() {
            super("test exception, expected");
        }
    }
    /**
     * Runtime wrapper around an {@link InterruptedException}.
     */
    protected static final class RuntimeInterruptedException extends RuntimeException {
        private static final long serialVersionUID = 8792817003755375675L;
        RuntimeInterruptedException(InterruptedException e) {
            super(e);
        }
    }

    protected static List<Callable<Object>> asCallables(List<? extends LoggingRunnable> runnables) {
        List<Callable<Object>> callables = new ArrayList<Callable<Object>>(runnables.size());
        for (LoggingRunnable runnable : runnables) {
            callables.add(runnable.asCallableReturningDefault());
        }
        return callables;
    }
    protected final void registerToClose(Closeable c) {
        toClose.add(c);
    }
    protected final ExecutorService newAncilliarySingleThreadedExecutor() {
        ThreadFactory threadFactory = getSubjectGenerator().getAncilliaryThreadFactory();
        return getSubjectGenerator().registerExecutor(newSingleThreadExecutor(threadFactory), threadFactory);
    }
}
