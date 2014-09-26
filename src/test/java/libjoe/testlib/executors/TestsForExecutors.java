package libjoe.testlib.executors;

import static libjoe.testlib.executors.ExecutorFeature.EXECUTOR;
import static libjoe.testlib.executors.ExecutorFeature.EXECUTOR_SERVICE;
import static libjoe.testlib.executors.ExecutorFeature.IGNORES_INTERRUPTS;
import static libjoe.testlib.executors.ExecutorFeature.LISTENING;
import static libjoe.testlib.executors.ExecutorFeature.NO_CONTROL_OF_THREAD_FACTORY;
import static libjoe.testlib.executors.ExecutorFeature.REJECTS_EXCESS_TASKS;
import static libjoe.testlib.executors.ExecutorFeature.SCHEDULED;
import static libjoe.testlib.executors.ExecutorFeature.SERIALISED_EXECUTION;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS_EXECUTE;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS_TASK_START;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;
import junit.framework.TestSuite;
import libjoe.testlib.executors.testers.InvokeAllTester;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.PackagePrivateAccessorForGuava;

public class TestsForExecutors {
	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite("tests for executors");

		suite.addTest(createTestsForJavaUtil());
		suite.addTest(createTestsForGuava());

		return suite;
	}

    static TestSuite createTestsForJavaUtil() throws Exception {
	    TestSuite javaUtil = new TestSuite("java.util");

		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return Executors.newSingleThreadExecutor(threadFactory);
			}
		}).named("Executors.newSingleThreadExecutor as a simple Executor").withFeatures(EXECUTOR, SERIALISED_EXECUTION).createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<Executor>() {
			@Override
			protected Executor createExecutor(ThreadFactory threadFactory) {
				return Executors.newSingleThreadExecutor(threadFactory);
			}
		}).named("Executors.newSingleThreadExecutor").withFeatures(EXECUTOR_SERVICE, SERIALISED_EXECUTION).createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return Executors.newCachedThreadPool(threadFactory);
			}
		}).named("Executors.newCachedThreadPool").withFeatures(EXECUTOR_SERVICE, SYNCHRONOUS_TASK_START).createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return Executors.newFixedThreadPool(2, threadFactory);
			}
		}).named("Executors.newFixedThreadPool[2 threads]").withFeatures(EXECUTOR_SERVICE).createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return Executors.newSingleThreadScheduledExecutor(threadFactory);
			}
		}).named("Executors.newSingleThreadedScheduledExecutor").withFeatures(SCHEDULED, EXECUTOR_SERVICE, SERIALISED_EXECUTION).createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return Executors.newScheduledThreadPool(2, threadFactory);
			}
		}).named("Executors.newScheduledThreadPool[core=2]").withFeatures(SCHEDULED, EXECUTOR_SERVICE).createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return new ThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3), threadFactory, new ThreadPoolExecutor.AbortPolicy());
			}
		}).named("TPE[core=0,max=1,queueCapacity=3,reh=abort]")
		    .withFeatures(EXECUTOR_SERVICE, REJECTS_EXCESS_TASKS)
    		.withMaxCapacity(4)
    		.withConcurrencyLevel(1)
    		.createTestSuite());

		// note the failing test in InvokeAllTester for these two
        javaUtil.addTest(newForkJoinPoolTestSuiteWithParallelism(1));
        javaUtil.addTest(newForkJoinPoolTestSuiteWithParallelism(2));
        javaUtil.addTest(newForkJoinPoolTestSuiteWithParallelism(3));

	    return javaUtil;
	}

    public static TestSuite newForkJoinPoolTestSuiteWithParallelism(final int parallelism) throws Exception {
        ExecutorTestSuiteBuilder<ExecutorService> fjpSuite = ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
            @Override
            protected ExecutorService createExecutor(final ThreadFactory threadFactory) {
                ForkJoinWorkerThreadFactory factory = new ForkJoinWorkerThreadFactory() {
                    @Override
                    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                        ForkJoinWorkerThread thread = new ForkJoinWorkerThread(pool) {
                            @Override
                            public void interrupt() {
                                notifyThreadInterrupted(this);
                                super.interrupt();
                            }
                        };
                        notifyNewThread(threadFactory, thread);
                        thread.setDaemon(true);
                        return thread;
                    }
                };
                return new ForkJoinPool(parallelism, factory, null, false);
            }
        }).named("ForkJoinPool[parallelism=" + parallelism + "]")
            .withFeatures(EXECUTOR_SERVICE, IGNORES_INTERRUPTS)
            .withConcurrencyLevel(parallelism);

        /*
         * This test fails sporadically, possibly more consistently with parallelism=2 than 3. ForkJoinPool#invokeAll cancels tasks when it
         * sees an exception. Whether this cancellation makes it into the returned future depends on a race condition in (parallel)
         * execution of the tasks.
         *
         * It's not completely clear whether this complies with the spec. The spec doesn't explicitly state that the tasks run
         * independently, but it feels odd for behaviour of later tasks to depend on earlier ones that threw an exception. That said,
         * perhaps fork-join should expect this sort of coupling between tasks, in which case cancellation of subsequent tasks may be
         * reasonable.
         */
        fjpSuite.suppressing(InvokeAllTester.class.getMethod("testInvokeAllMixedCompletesAllTasks_NoTimeout"));

        return fjpSuite.createTestSuite();
    }

	static TestSuite createTestsForGuava() {
        TestSuite guava = new TestSuite("guava");

        guava.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ListeningExecutorService>() {
                    @Override
                    protected ListeningExecutorService createExecutor(ThreadFactory threadFactory) {
                        return MoreExecutors.newDirectExecutorService();
                    }
                })
            .named("MoreExecutors.newDirectExecutorService")
            .withFeatures(LISTENING, EXECUTOR_SERVICE, SYNCHRONOUS, SERIALISED_EXECUTION, NO_CONTROL_OF_THREAD_FACTORY)
            .createTestSuite());
        guava.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<Executor>() {
                    @Override
                    protected Executor createExecutor(ThreadFactory threadFactory) {
                        return MoreExecutors.directExecutor();
                    }
                })
            .named("MoreExecutors.directExecutor")
            .withFeatures(EXECUTOR, SYNCHRONOUS, SERIALISED_EXECUTION, NO_CONTROL_OF_THREAD_FACTORY)
            .createTestSuite());
		guava.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<Executor>() {
				    @Override
				    protected Executor createExecutor(ThreadFactory threadFactory) {
				        return PackagePrivateAccessorForGuava.newSerializingExecutor(MoreExecutors.directExecutor());
				    }
				})
			.named("SerializingExecutor")
			.withFeatures(EXECUTOR, SYNCHRONOUS_EXECUTE, SERIALISED_EXECUTION)
			.createTestSuite());

        return guava;
    }
}
