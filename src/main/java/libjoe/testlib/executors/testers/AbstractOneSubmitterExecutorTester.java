package libjoe.testlib.executors.testers;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import libjoe.testlib.executors.ExecutorSubmitter;
import libjoe.testlib.executors.ExecutorTestSuiteBuilder.OneSubmitterTestSuiteGenerator;
import libjoe.testlib.executors.LoggingRunnable;

public class AbstractOneSubmitterExecutorTester<E extends Executor> extends AbstractExecutorTester<E, OneSubmitterTestSuiteGenerator<E>> {
    /**
     * Submits the task to the executor in the manner specified by the {@link #getSubmitter()}, returning the future for submission.
     */
    protected final Future<?> submit(E executor, LoggingRunnable task) throws InterruptedException {
        return getSubmitter().submit(executor, task);
    }
    /**
     * Submits the task to the executor in the manner specified by the {@link #getSubmitter()} from another thread.
     */
    protected final void submitAsync(final E executor, final LoggingRunnable task) throws InterruptedException {
        newAncilliarySingleThreadedExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                getSubmitter().submit(executor, task);
                return null;
            }
        });
    }

    protected final void addTasksToCapacity(E executor) throws InterruptedException, BrokenBarrierException, TimeoutException {
        addTasksToCapacity(executor, getSubmitter());
    }
    protected final ExecutorSubmitter<E> getSubmitter() {
        return getSubjectGenerator().getSubmitter();
    }

    protected final ExecutorService newAncilliarySingleThreadedExecutor() {
        ThreadFactory threadFactory = getSubjectGenerator().getAncilliaryThreadFactory();
        return getSubjectGenerator().registerExecutor(newSingleThreadExecutor(threadFactory), threadFactory);
    }
}
