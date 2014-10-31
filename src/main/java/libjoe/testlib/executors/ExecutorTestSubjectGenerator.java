package libjoe.testlib.executors;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import libjoe.testlib.executors.testers.AbstractExecutorTester;

import com.google.common.collect.Sets;
import com.google.common.collect.testing.TestSubjectGenerator;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public abstract class ExecutorTestSubjectGenerator<E extends Executor> implements TestSubjectGenerator<E> {
	public static final int UNASSIGNED = -1;

	private ThreadFactory threadFactory;
	private ThreadFactory ancilliaryThreadFactory;
	private final Queue<Executor> liveExecutors = new ConcurrentLinkedQueue<>();
    private final Map<Thread, ThreadFactory> thread2Factory = new ConcurrentHashMap<>();
    private final Map<ThreadFactory, Executor> threadFactory2Executor = new ConcurrentHashMap<>();
    private final Set<Thread> interruptedThreads = Sets.newConcurrentHashSet();

    private int maxCapacity = UNASSIGNED;
    private int concurrencyLevel = UNASSIGNED;


	public final void setTester(AbstractExecutorTester<E, ? extends ExecutorTestSubjectGenerator<E>> currentTester) {
		this.threadFactory = newThreadFactory("executor-tester-[" + currentTester.getName() + "." + currentTester.getTestMethodName() + "]-%s");
		this.ancilliaryThreadFactory = newThreadFactory("executor-ancilliary-[" + currentTester.getName() + "." + currentTester.getTestMethodName() + "]-%s");
	}
    protected ThreadFactory newThreadFactory(String nameFormat) {
        final AtomicReference<ThreadFactory> factoryReference = new AtomicReference<>();
        ThreadFactory factory = new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat(nameFormat)
				.setThreadFactory(new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						Thread thread = new InterruptRecordingThread(r);
						ThreadFactory factory = factoryReference.get();
                        notifyNewThread(factory, thread);
                        return thread;
					}
				})
				.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						// ignore
					}
				})
				.build();
        factoryReference.set(factory);
        return factory;
    }
    final ExecutorTestSubjectGenerator<E> withMaxCapicity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        return this;
    }
    final ExecutorTestSubjectGenerator<E> withConcurrencyLevel(int concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
        return this;
    }

	@Override
	public final E createTestSubject() {
		return registerExecutor(createExecutor(threadFactory), threadFactory);
	}
    public final <E2 extends Executor> E2 registerExecutor(E2 executor) {
        return executor;
    }
    public final <E2 extends Executor> E2 registerExecutor(E2 executor, ThreadFactory threadFactory) {
        liveExecutors.add(executor);
        threadFactory2Executor.put(threadFactory, executor);
        return executor;
    }
    public final ThreadFactory getAncilliaryThreadFactory() {
        return ancilliaryThreadFactory;
    }

	public final void tearDown() {
		while (!liveExecutors.isEmpty()) {
			close(liveExecutors.poll());
		}
	}
	private final void close(Executor usedExecutor) {
		if (usedExecutor != null && usedExecutor instanceof ExecutorService) {
			((ExecutorService) usedExecutor).shutdownNow();
		}
	}
	public final Executor getExecutorForThread(Thread t) {
	    ThreadFactory factory = thread2Factory.get(t);
	    checkNotNull(factory, "No registered ThreadFactory found for thread %s", t);
	    Executor executor = threadFactory2Executor.get(factory);
	    checkNotNull(executor, "No registered Executor found for thread %s. Factory: %s", t, factory);
	    return executor;
	}

    /**
     * Creates the {@link Executor} under test. The given {@link ThreadFactory} should be used by the created executor, unless
     * {@link ExecutorFeature#NO_CONTROL_OF_THREAD_FACTORY} is specified as a feature of the executor.
     *
     * @param threadFactory {@link ThreadFactory} to be used by the executor
     * @return the executor to test
     */
	protected abstract E createExecutor(ThreadFactory threadFactory);

	public final int getMaxCapacity() {
        return maxCapacity;
    }
	public final int getConcurrencyLevel() {
        return concurrencyLevel;
    }
	public final int getMaxQueuedCapacity() {
		return maxCapacity == UNASSIGNED ? UNASSIGNED : maxCapacity - concurrencyLevel;
	}

	private final class InterruptRecordingThread extends Thread {
		InterruptRecordingThread(Runnable r) {
			super(r);
		}

		@Override
		public void interrupt() {
		    notifyThreadInterrupted(this);
			super.interrupt();
		}
	}
	public final boolean wasInterrupted(@Nullable Thread thread) {
		return interruptedThreads.contains(thread);
	}
    public final void notifyThreadInterrupted(Thread thread) {
        interruptedThreads.add(thread);
    }
    public final void notifyNewThread(ThreadFactory factory, Thread thread) {
        thread2Factory.put(thread, factory);
    }
}
