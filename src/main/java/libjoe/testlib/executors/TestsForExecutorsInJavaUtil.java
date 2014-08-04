package libjoe.testlib.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.MoreExecutors;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestsForExecutorsInJavaUtil {
	public static Test suite() {
		TestSuite suite = new TestSuite("executors in java.util");

		suite.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
			@Override
			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
				return Executors.newSingleThreadExecutor(threadFactory);
			}
		}).named("Executors.newSingleThreadExecutor").createTestSuite());
//		suite.addTest(ExecutorTestSuiteBuilder.using(new ExecutorTestSubjectGenerator<ExecutorService>() {
//			@Override
//			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
//				return Executors.newCachedThreadPool(threadFactory);
//			}
//		}).named("Executors.newCachedThreadPool").createTestSuite());
//		suite.addTest(ExecutorTestSuiteBuilder.using(new ExecutorServiceTestSubjectGenerator<ExecutorService>() {
//			@Override
//			protected ExecutorService createExecutor(ThreadFactory threadFactory) {
//				return MoreExecutors.sameThreadExecutor();
//			}
//		}).named("MoreExecutors.sameThreadExecutor").withFeatures(ExecutorFeature.EXECUTOR_SERVICE).createTestSuite());
		
		return suite;
	}
}
