package libjoe.testlib.executors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import junit.framework.TestSuite;
import libjoe.testlib.executors.testers.BasicShutdownTester;
import libjoe.testlib.executors.testers.CancellationTester;
import libjoe.testlib.executors.testers.ExecuteTester;
import libjoe.testlib.executors.testers.InvokeAllTester;
import libjoe.testlib.executors.testers.InvokeAnyTester;
import libjoe.testlib.executors.testers.ListenableFutureTester;
import libjoe.testlib.executors.testers.ShutdownTasksTester;
import libjoe.testlib.executors.testers.SubmitRejectedTester;
import libjoe.testlib.executors.testers.SubmitTester;

import com.google.common.collect.Sets;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.Feature;
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
                ExecuteTester.class, InvokeAllTester.class, InvokeAnyTester.class, BasicShutdownTester.class
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
        checkArgument(maxCapacity == ExecutorTestSubjectGenerator.UNASSIGNED || maxCapacity >= 3, "A couple of tests assume capacity larger than 3. Please pick a larger value.");
        getSubjectGenerator().withMaxCapicity(maxCapacity);
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
        getSubjectGenerator().withConcurrencyLevel(concurrencyLevel);
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

        addSubmitterFeatureTestSuites(testSuite);

        if (getFeatures().contains(ExecutorFeature.EXECUTOR_SERVICE) && !getFeatures().contains(ExecutorFeature.LISTENING)) {
            TestSuite derivedTestSuiteForListenableDecorator = createDerivedTestSuiteForListenableDecorator(testSuite);
            testSuite.addTest(derivedTestSuiteForListenableDecorator);
        }

        return testSuite;
    }

    private static final Set<ExecutorServiceSubmitters> ALL_ES_SUBMITTERS = Sets.immutableEnumSet(EnumSet.allOf(ExecutorServiceSubmitters.class));
    private static final Set<ScheduledExecutorServiceSubmitters> ALL_SES_SUBMITTERS = Sets.immutableEnumSet(EnumSet.allOf(ScheduledExecutorServiceSubmitters.class));
    private void addSubmitterFeatureTestSuites(TestSuite testSuite) {
        if (getFeatures().contains(ExecutorFeature.EXECUTOR_SERVICE)) {
            addTestsForSubmitters(testSuite, ALL_ES_SUBMITTERS);
        }
        if (getFeatures().contains(ExecutorFeature.SCHEDULED)) {
            addTestsForSubmitters(testSuite, ALL_SES_SUBMITTERS);
        }
    }

    /**
     * Creates a derived test suite over the invocation options implemented by the set of submitters. If the submission options are not
     * compatible with the
     *
     * @param superTestSuite test suite into which to write the generated tests
     * @param allSubmitters set of execution submitters for this type of executor
     * @param <E2> refined type of the executor. This is a subtype of {@code E}; we can't enforce this in the type system but the presence
     *        of a feature is evidence for it.
     * @param <ES> type of the submitter to the executor, likely one of the enums implementing {@link ExecutorSubmitter}
     */
    private <E2 extends Executor, ES extends Feature<E2> & ExecutorSubmitter<E2>> void addTestsForSubmitters(TestSuite superTestSuite, Set<ES> allSubmitters) {
        // the presence of the features in the set says this cast works, because E2 <: E
        @SuppressWarnings("unchecked")
        ExecutorTestSubjectGenerator<E2> castSubjectGenerator = (ExecutorTestSubjectGenerator<E2>) getSubjectGenerator();

        for (ES submitter : allSubmitters) {
            OneSubmitterTestSuiteGenerator<E2> oneMethodGenerator = new OneSubmitterTestSuiteGenerator<>(castSubjectGenerator, submitter);
            String oneMethodName = getName() + " [submitter: " + submitter + "]";

            Set<Feature<?>> oneSubmitterFeatures = Helpers.copyToSet(getFeatures());
            oneSubmitterFeatures.removeAll(allSubmitters);
            oneSubmitterFeatures.add(submitter);

            OneSubmitterTestSuiteBuilder<E2> builder = new OneSubmitterTestSuiteBuilder<>(oneMethodGenerator)
                .named(oneMethodName)
                .withFeatures(oneSubmitterFeatures)
                .withSetUp(getSetUp())
                .withTearDown(getTearDown())
                .suppressing(getSuppressedTests());
            TestSuite oneSubmitterTestSuite = builder.createTestSuite();
            if (oneSubmitterTestSuite.countTestCases() > 0) {
                superTestSuite.addTest(oneSubmitterTestSuite);
            }
        }
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

    private static final class OneSubmitterTestSuiteBuilder<E extends Executor> extends
            FeatureSpecificTestSuiteBuilder<OneSubmitterTestSuiteBuilder<E>, OneSubmitterTestSuiteGenerator<E>> {
        OneSubmitterTestSuiteBuilder(OneSubmitterTestSuiteGenerator<E> oneMethodGenerator) {
            usingGenerator(oneMethodGenerator);
        }
        @SuppressWarnings("rawtypes")
        @Override
        protected List<Class<? extends AbstractTester>> getTesters() {
            return Arrays.<Class<? extends AbstractTester>>asList(
                    SubmitTester.class, SubmitRejectedTester.class, CancellationTester.class,
                    ListenableFutureTester.class, ShutdownTasksTester.class
                    );
        }
    }
    public static final class OneSubmitterTestSuiteGenerator<E extends Executor> extends ExecutorTestSubjectGenerator<E> {
        private final ExecutorTestSubjectGenerator<? extends E> backingGenerator;
        private final ExecutorSubmitter<E> submitter;

        public OneSubmitterTestSuiteGenerator(ExecutorTestSubjectGenerator<? extends E> backingGenerator, ExecutorSubmitter<E> submitter) {
            this.backingGenerator = backingGenerator;
            this.submitter = submitter;

            withConcurrencyLevel(backingGenerator.getConcurrencyLevel());
            withMaxCapicity(backingGenerator.getMaxCapacity());
        }

        @Override
        protected E createExecutor(ThreadFactory threadFactory) {
            return backingGenerator.createExecutor(threadFactory);
        }
        public ExecutorSubmitter<E> getSubmitter() {
            return submitter;
        }
    }
}
