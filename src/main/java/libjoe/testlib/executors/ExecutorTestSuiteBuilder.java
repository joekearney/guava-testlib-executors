package libjoe.testlib.executors;

import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import junit.framework.TestSuite;
import libjoe.testlib.executors.testers.CancellationTester;
import libjoe.testlib.executors.testers.ExecuteTester;
import libjoe.testlib.executors.testers.SubmitRejectedTester;
import libjoe.testlib.executors.testers.SubmitTester;

import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public final class ExecutorTestSuiteBuilder<E extends Executor> extends FeatureSpecificTestSuiteBuilder<ExecutorTestSuiteBuilder<E>, ExecutorTestSubjectGenerator<E>> {
	private ExecutorTestSuiteBuilder(ExecutorTestSubjectGenerator<E> generator) {
		usingGenerator(generator);
	}

	@SuppressWarnings("rawtypes") // just how it is
	@Override
	protected List<Class<? extends AbstractTester>> getTesters() {
		return Arrays.<Class<? extends AbstractTester>>asList(
				ExecuteTester.class, SubmitTester.class, SubmitRejectedTester.class, CancellationTester.class
				);
	}

	public static <E extends Executor> ExecutorTestSuiteBuilder<E> using(ExecutorTestSubjectGenerator<E> generator) {
		return new ExecutorTestSuiteBuilder<E>(generator);
	}
	/**
	 * Specifies the max total capacity of the executor under test, the sum of running and queued tasks.
	 * 
	 * @param maxCapacity max running + max queued capacity
	 * @return this builder
	 */
	// TODO something more sophisticated will be required to cope with cases where maxCapacity != running + queued
	public ExecutorTestSuiteBuilder<E> withMaxCapacity(int maxCapacity) {
		getSubjectGenerator().setMaxCapicity(maxCapacity);
		return this;
	}
	/**
	 * Specifies the max number of tasks that can be running at any one time, required for testing {@link ExecutorFeature#REJECTS_EXCESS_TASKS}.
	 * 
	 * @param concurrencyLevel number of tasks that can be executed in parallel
	 * @return this builder
	 */
	// TODO something more sophisticated will be required to cope with cases where maxCapacity != running + queued
    public ExecutorTestSuiteBuilder<E> withConcurrencyLevel(int concurrencyLevel) {
        getSubjectGenerator().setConcurrencyLevel(concurrencyLevel);
        return this;
    }
	
	@Override
	public TestSuite createTestSuite() {
	    if (getFeatures().contains(ExecutorFeature.REJECTS_EXCESS_TASKS)) {
            checkState(getSubjectGenerator().getMaxCapacity() != ExecutorTestSubjectGenerator.UNASSIGNED,
                    "If you want to test for REJECTS_EXCESS_TASKS in " + getName() + ", you need to give a maxCapacity on the test suite builder.");
            checkState(getSubjectGenerator().getConcurrencyLevel() != ExecutorTestSubjectGenerator.UNASSIGNED,
                    "If you want to test for REJECTS_EXCESS_TASKS in " + getName() + ", you need to give a concurrencyLevel on the test suite builder.");
	    }
	    
		TestSuite testSuite = super.createTestSuite();
		
		if (getFeatures().contains(ExecutorFeature.EXECUTOR_SERVICE) && !getFeatures().contains(ExecutorFeature.LISTENING)) {
			TestSuite derivedTestSuiteForListenableDecorator = createDerivedTestSuiteForListenableDecorator(testSuite);
			testSuite.addTest(derivedTestSuiteForListenableDecorator);
		}
		
		return testSuite;
	}

	private TestSuite createDerivedTestSuiteForListenableDecorator(TestSuite testSuite) {
		final ExecutorTestSuiteBuilder<? extends ListeningExecutorService> derivedBuilder;
		
		if (getFeatures().contains(ExecutorFeature.SCHEDULED)) {
            derivedBuilder = ExecutorTestSuiteBuilder.<ListeningScheduledExecutorService>using(
			        new ExecutorTestSubjectGenerator<ListeningScheduledExecutorService>() {
        				@Override
        				protected ListeningScheduledExecutorService createExecutor(ThreadFactory threadFactory) {
        					return MoreExecutors.listeningDecorator((ScheduledExecutorService) getSubjectGenerator().createExecutor(threadFactory));
        				}
        			})
        			.named("ListenableScheduled[" + getName() + "]");
		} else {
			derivedBuilder = ExecutorTestSuiteBuilder.<ListeningExecutorService>using(
			        new ExecutorTestSubjectGenerator<ListeningExecutorService>() {
        				@Override
        				protected ListeningExecutorService createExecutor(ThreadFactory threadFactory) {
        					return MoreExecutors.listeningDecorator((ExecutorService) getSubjectGenerator().createExecutor(threadFactory));
        				}
        			})
        			.named("Listenable[" + getName() + "]");
		}
		
		return derivedBuilder
		        .withFeatures(getFeatures())
		        .withConcurrencyLevel(getSubjectGenerator().getConcurrencyLevel())
		        .withMaxCapacity(getSubjectGenerator().getMaxCapacity())
		        .withFeatures(ExecutorFeature.LISTENING)
		        .createTestSuite();
	}
}
