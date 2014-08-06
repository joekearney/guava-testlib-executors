package libjoe.testlib.executors;

import static libjoe.testlib.executors.ExecutorFeature.EXECUTOR;
import static libjoe.testlib.executors.ExecutorFeature.EXECUTOR_SERVICE;
import static libjoe.testlib.executors.ExecutorFeature.LISTENING;
import static libjoe.testlib.executors.ExecutorFeature.NO_CONTROL_OF_THREAD_FACTORY;
import static libjoe.testlib.executors.ExecutorFeature.REJECTS_EXCESS_TASKS;
import static libjoe.testlib.executors.ExecutorFeature.SCHEDULED;
import static libjoe.testlib.executors.ExecutorFeature.SERIALISED_EXECUTION;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS_EXECUTE;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.PackagePrivateAccessorForGuava;

public class TestsForExecutors {
	public static Test suite() {
		TestSuite suite = new TestSuite("tests for executors");

		suite.addTest(createTestsForJavaUtil());
		suite.addTest(createTestsForGuava());
		
		return suite;
	}
	
    static TestSuite createTestsForGuava() {
        TestSuite guava = new TestSuite("guava");
		
		guava.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ListeningExecutorService>() {
					@Override
					protected ListeningExecutorService createExecutor(ThreadFactory threadFactory) {
						return MoreExecutors.sameThreadExecutor();
					}
				})
			.named("MoreExecutors.sameThreadExecutor")
			.withFeatures(LISTENING, EXECUTOR_SERVICE, SYNCHRONOUS, SERIALISED_EXECUTION, NO_CONTROL_OF_THREAD_FACTORY)
			.createTestSuite());
		guava.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<Executor>() {
				    @Override
				    protected Executor createExecutor(ThreadFactory threadFactory) {
				        return PackagePrivateAccessorForGuava.newSerializingExecutor(MoreExecutors.sameThreadExecutor());
				    }
				})
			.named("SerializingExecutor")
			.withFeatures(EXECUTOR, SYNCHRONOUS_EXECUTE, SERIALISED_EXECUTION)
			.createTestSuite());
		
        return guava;
    }

    static TestSuite createTestsForJavaUtil() {
        TestSuite javaUtil = new TestSuite("java.util");
		
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return Executors.newSingleThreadExecutor(threadFactory);
			}
		}).named("Executors.SingleThreadExecutor as a simple Executor").withFeatures(EXECUTOR, SERIALISED_EXECUTION).createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<Executor>() {
			@Override
			protected Executor createExecutor(ThreadFactory threadFactory) {
				return Executors.newSingleThreadExecutor(threadFactory);
			}
		}).named("Executors.newSingleThreadExecutor").withFeatures(EXECUTOR_SERVICE, SERIALISED_EXECUTION).createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return new ThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3), threadFactory, new ThreadPoolExecutor.AbortPolicy());
			}
		}).named("TPE[core=0,max=1,queueCapacity=3,reh=abort]").withFeatures(EXECUTOR_SERVICE, REJECTS_EXCESS_TASKS)
		    .withMaxCapacity(4)
		    .withConcurrencyLevel(1)
		    .createTestSuite());
		javaUtil.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return Executors.newCachedThreadPool(threadFactory);
			}
		}).named("Executors.newCachedThreadPool").withFeatures(EXECUTOR_SERVICE).createTestSuite());
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
        return javaUtil;
    }
}
