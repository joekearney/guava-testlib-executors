package libjoe.testlib.executors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import libjoe.testlib.executors.testers.CancellationTester;
import libjoe.testlib.executors.testers.ExecuteTester;
import libjoe.testlib.executors.testers.SubmitTester;

import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;

public class ExecutorTestSuiteBuilder<E extends Executor> extends
		FeatureSpecificTestSuiteBuilder<ExecutorTestSuiteBuilder<E>, ExecutorTestSubjectGenerator<E>> {

	private ExecutorTestSuiteBuilder(ExecutorTestSubjectGenerator<E> generator) {
		usingGenerator(generator);
	}

	@SuppressWarnings("rawtypes") // just how it is
	@Override
	protected List<Class<? extends 
			AbstractTester>> getTesters() {
		return Arrays.<Class<? extends AbstractTester>>asList(
				ExecuteTester.class, SubmitTester.class, CancellationTester.class
				);
	}

	public static <E extends Executor> ExecutorTestSuiteBuilder<E> using(ExecutorTestSubjectGenerator<E> generator) {
		return new ExecutorTestSuiteBuilder<E>(generator);
	}
}
