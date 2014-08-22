package libjoe.testlib.executors.testers;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import libjoe.testlib.executors.ExecutorSubmitter;
import libjoe.testlib.executors.ExecutorTestSuiteBuilder.OneSubmitterTestSuiteGenerator;
import libjoe.testlib.executors.LoggingRunnable;

public class AbstractOneSubmitterExecutorTester<E extends Executor> extends AbstractExecutorTester<E> {
    @Override
    public OneSubmitterTestSuiteGenerator<E> getSubjectGenerator() {
        return (OneSubmitterTestSuiteGenerator<E>) super.getSubjectGenerator();
    }

    protected final Future<?> submit(E executor, LoggingRunnable task) throws InterruptedException {
        return getSubjectGenerator().getSubmitter().submit(executor, task);
    }

    protected final void addTasksToCapacity(E executor) throws InterruptedException, BrokenBarrierException, TimeoutException {
        addTasksToCapacity(executor, getSubjectGenerator().getSubmitter());
    }

    protected final ExecutorSubmitter getSubmitter() {
        return getSubjectGenerator().getSubmitter();
    }

    protected final ExecutorService newAncilliarySingleThreadedExecutor() {
        ThreadFactory threadFactory = getSubjectGenerator().getAncilliaryThreadFactory();
        return getSubjectGenerator().registerExecutor(newSingleThreadExecutor(threadFactory), threadFactory);
    }
}
