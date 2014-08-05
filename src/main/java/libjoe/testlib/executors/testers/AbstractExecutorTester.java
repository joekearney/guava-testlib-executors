package libjoe.testlib.executors.testers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.lang.Thread.State;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.AssertionFailedError;
import libjoe.testlib.executors.ExecutorTestSubjectGenerator;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.testing.AbstractTester;

public abstract class AbstractExecutorTester<E extends Executor> extends AbstractTester<ExecutorTestSubjectGenerator<E>> {
	protected static final Object RETURN_VALUE = new Object();

	@Override
	public ExecutorTestSubjectGenerator<E> getSubjectGenerator() {
		return super.getSubjectGenerator();
	}

	@Override
	public final void setUp() throws Exception {
		getSubjectGenerator().setTester(this);
	}
	@Override
	public final void tearDown() throws Exception {
		getSubjectGenerator().tearDown();
	}

	protected final void addTasksToCapacity(E executor, ExecutorSubmitter submitter) throws InterruptedException, BrokenBarrierException, TimeoutException {
        int maxCapacity = getSubjectGenerator().getMaxCapacity();
        int concurrencyLevel = getSubjectGenerator().getConcurrencyLevel();
    
        List<RunnableWithBarrier> runningTasks = createManyRunnablesWithBarriers(concurrencyLevel, 2, 2);
        List<RunnableWithBarrier> queuedTasks = createManyRunnablesWithBarriers(maxCapacity - concurrencyLevel, 2, 1);
    
        for (RunnableWithBarrier task : runningTasks) {
            submitter.submit(executor, task);
            task.awaitDefault();
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

    protected final <T> void assertThrows(Future<T> future, Class<? extends Throwable> type) {
		try {
			future.get(getTimeoutDuration(), getTimeoutUnit());
			fail("Should have thrown");
		} catch (ExecutionException e) {
			// pass if it's the right type
			assertThat("Wrong type of cause exception for the ExecutionException", e.getCause(), instanceOf(type));
		} catch (Throwable e) {
			AssertionFailedError afe = new AssertionFailedError("Threw wrong type of exception. Expected an ExecutionException.");
			afe.initCause(e);
			throw afe;
		}
	}
	protected final void checkTaskRan(LoggingRunnable runnable) {
		try {
			runnable.awaitRun(getTimeoutDuration(), getTimeoutUnit());
		} catch (InterruptedException e) {
			throw new RuntimeException("Thread was interrupted while waiting for task to finish", e);
		}
		assertThat("Task should have been executed, but it didn't run.", runnable.wasRun());
	}
	protected final void checkCompletedFuture(LoggingRunnable task, Future<?> future, Object expected) throws InterruptedException, ExecutionException,
			TimeoutException {
		Object result = future.get(getTimeoutDuration(), getTimeoutUnit());
		assertThat("Task should have been executed, but it didn't run.", task.wasRun());
		assertThat("Future should be isDone() after get() returns", future.isDone());
		assertThat("Future should not be isCancelled() after get() returns", !future.isCancelled());
		assertThat("Future should return expected value", result, is(sameInstance(expected)));
	}
	protected final void checkFutureAfterExecutionException(Future<?> future) {
		assertThrows(future, RuntimeRunnableException.class);
		assertThat("Future should be isDone() after get() throws", future.isDone());
		assertThat("Future should not be isCancelled() after get() throws", !future.isCancelled());
	}
	protected final void checkCancelledFuture(Future<?> future) throws InterruptedException, ExecutionException, TimeoutException {
		try {
			future.get(getTimeoutDuration(), getTimeoutUnit());
			fail("Cancelled task should have thrown cancellation exception");
		} catch (CancellationException e) {
			// pass
		}
		assertThat("Future should be isCancelled() after cancellation", future.isCancelled());
		assertThat("Future should be isDone() after cancellation and completion", future.isDone());
	}
    private static final Set<Thread.State> BLOCKING_STATES = Sets.immutableEnumSet(State.BLOCKED, State.WAITING, State.TIMED_WAITING);
    protected final void checkTaskIsBlocked(LoggingRunnable runningTask) {
        Thread thread = runningTask.getRunningThread();
        assertThat("Task was expected to be in blocked state, but it hasn't started yet. Task: " + runningTask, thread != null);
        State state = thread.getState();
        assertThat("Expected task in a blocked state (one of " + BLOCKING_STATES + ") but it was " + state, BLOCKING_STATES.contains(state));
    }

	protected final long getTimeoutDuration() {
		// TODO make this dependent on features of the test subject? longer for "really" async stuff, if required
		return 1;
	}
	protected final TimeUnit getTimeoutUnit() {
		return TimeUnit.SECONDS;
	}
	
	protected static interface LoggingRunnable extends Runnable {
		Callable<Object> asCallableReturningValue();
		Callable<Object> asCallableReturningNothing();
		boolean wasInterrupted();
		boolean wasRun();
		boolean hasFinished();
		boolean awaitRun(long timeout, TimeUnit unit) throws InterruptedException;
        Thread getRunningThread();
	}
	protected abstract class AbstractLoggingRunnable implements LoggingRunnable {
		private final CountDownLatch latchForFirstRun = new CountDownLatch(1);
		private volatile boolean ran = false;
		private volatile boolean finished = false;
		private volatile Thread runningThread = null;
		
		@Override
		public final synchronized void run() {
			ran = true;
			latchForFirstRun.countDown();
			try {
    			Thread thisThread = Thread.currentThread();
    			checkState(runningThread == null, "Runnable should be executed only once. Already executed on thread[%s], tried again on thread [%s]", runningThread, thisThread.getName());
    			runningThread = thisThread;
//    			logRunning(thisThread);
    			doRun();
			} finally {
			    finished = true;
			}
		}
		
		@Override
		public final Callable<Object> asCallableReturningValue() {
			return Executors.callable(this, RETURN_VALUE);
		}
		@Override
		public final Callable<Object> asCallableReturningNothing() {
			return Executors.callable(this);
		}

		@Override
		public final boolean wasInterrupted() {
			return ExecutorTestSubjectGenerator.wasInterrupted(runningThread);
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
		public boolean awaitRun(long timeout, TimeUnit unit) throws InterruptedException {
			return latchForFirstRun.await(timeout, unit);
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
        ImmutableList.Builder<RunnableWithBarrier> tasks = ImmutableList.builder();
        for (int i = 0; i < numToCreate; i++) {
            tasks.add(new RunnableWithBarrier(parties, rounds));
        }
        return tasks.build();
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
		
		public final void await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
			barrier.await(timeout, unit);
		}
		public final void awaitDefault() throws InterruptedException, BrokenBarrierException, TimeoutException {
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
				throw new RuntimeException(e);
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
		    return Objects.toStringHelper(this).add("roundsCompleted", roundsCompleted).add("of", rounds).toString();
		}
	}
	/** Creates a {@link Runnable} that does nothing. */
	protected final NoopRunnable noopRunnable() {
		return new NoopRunnable();
	}
	private final class NoopRunnable extends AbstractLoggingRunnable {
		@Override
		void doRun() {}
	}
	/** Creates a {@link Runnable} that throws {@link RuntimeRunnableException}. */
	protected final ThrowingRunnable throwingRunnable() {
		return new ThrowingRunnable();
	}

    private final class ThrowingRunnable extends AbstractLoggingRunnable {
		@Override
		void doRun() throws RuntimeRunnableException {
			throw new RuntimeRunnableException();
		}
	}
	protected static final class RuntimeRunnableException extends RuntimeException {
		private static final long serialVersionUID = 8792817003755375674L;
		RuntimeRunnableException() {
			super("test exception, expected");
		}
	}

	/**
	 * Abstraction over the three methods {@link ExecutorService#submit(Runnable)}, {@link ExecutorService#submit(Runnable, Object)}, {@link ExecutorService#submit(Callable)}.
	 */
	protected interface ExecutorSubmitter {
		Future<?> submit(Executor executor, LoggingRunnable runnable) throws InterruptedException;

		Object getExpectedValue();
	}
	protected enum ExecutorSubmitters implements ExecutorSubmitter {
	    /**
	     * Uses {@link Executor#execute(Runnable)}, returns a {@code null} {@link Future}.
	     */
	    EXECUTE {
	        @Override
	        public Future<?> submit(Executor executor, LoggingRunnable runnable) {
	            executor.execute(runnable);
	            return null;
	        }
	        @Override
	        public Object getExpectedValue() {
	            return null;
	        }
	    },
		RUNNABLE {
			@Override
			public Future<?> submit(Executor executor, LoggingRunnable runnable) {
				return ((ExecutorService) executor).submit(runnable);
			}
			@Override
			public Object getExpectedValue() {
				return null;
			}
		},
		CALLABLE {
			@Override
			public Future<?> submit(Executor executor, LoggingRunnable runnable) {
				return ((ExecutorService) executor).submit(runnable.asCallableReturningValue());
			}
		},
		RUNNABLE_WITH_VALUE {
			@Override
			public Future<?> submit(Executor executor, LoggingRunnable runnable) {
				return ((ExecutorService) executor).submit(runnable, AbstractExecutorTester.RETURN_VALUE);
			}
		},
		INVOKE_ALL {
		    @Override
		    public Future<?> submit(Executor executor, LoggingRunnable runnable) throws InterruptedException {
		        List<Future<Object>> allFutures = ((ExecutorService) executor).invokeAll(Arrays.asList(runnable.asCallableReturningValue()));
		        return Iterables.getOnlyElement(allFutures);
		    }
		}
		;
		@Override
		public Object getExpectedValue() {
		    return RETURN_VALUE;
		}
	}
}
