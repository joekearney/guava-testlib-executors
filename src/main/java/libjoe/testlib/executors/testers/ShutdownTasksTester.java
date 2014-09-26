package libjoe.testlib.executors.testers;

import static libjoe.testlib.executors.ExecutorFeature.IGNORES_INTERRUPTS;
import static libjoe.testlib.executors.ExecutorFeature.SHUTDOWN_SUPPRESSED;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS_EXECUTE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.LoggingRunnable;

/**
 * Tests for shutdown after task submission.
 *
 * @author Joe Kearney
 */
@Require(value=ExecutorFeature.EXECUTOR_SERVICE)
public class ShutdownTasksTester<E extends ExecutorService> extends AbstractOneSubmitterExecutorTester<E> {
    @Require(absent={SHUTDOWN_SUPPRESSED})
    public void testShutdownAfterTaskCompletion() throws Exception {
        E executor = createExecutor();
        LoggingRunnable task = noopRunnable();
        Future<?> future = submit(executor, task);
        // await completion
        checkCompletedFuture(task, future, getSubmitter().getExpectedValue());

        assertFalse("Executor should not be isShutdown() before shutdown()", executor.isShutdown());
        assertFalse("Executor should not be isTerminated() before shutdown()", executor.isTerminated());

        executor.shutdown();

        assertThat("Executor should be isShutdown() after shutdown", executor.isShutdown());
        awaitTermination(executor);
        assertThat("Executor should be isTerminated() after shutdown", executor.isTerminated());
    }
    @Require(absent={SHUTDOWN_SUPPRESSED})
    public void testShutdownNowAfterTaskCompletion() throws Exception {
        E executor = createExecutor();
        LoggingRunnable task = noopRunnable();
        Future<?> future = submit(executor, task);
        // await completion
        checkCompletedFuture(task, future, getSubmitter().getExpectedValue());

        assertFalse("Executor should not be isShutdown() before shutdownNow()", executor.isShutdown());
        assertFalse("Executor should not be isTerminated() before shutdownNow()", executor.isTerminated());

        List<Runnable> tasksOutstanding = executor.shutdownNow();

        assertThat("Executor should be isShutdown() after shutdown on empty executor", executor.isShutdown());
        awaitTermination(executor);
        assertThat("Should be no tasks outstanding after shutdownNow() on empty executor", tasksOutstanding.isEmpty());
        assertThat("Executor should be isTerminated() after shutdown on empty executor", executor.isTerminated());
    }

    @Require(absent={SYNCHRONOUS_EXECUTE, SHUTDOWN_SUPPRESSED})
    public void testAwaitTerminationTimesOutIfTaskRunning_Shutdown() throws Exception {
        E executor = createExecutor();
        UninterruptibleRunnable task = new UninterruptibleRunnable();
        Future<?> future = submit(executor, task);
        task.awaitRunningDefault();

        executor.shutdown();
        assertTrue("Executor should be isShutdown() after shutdown", executor.isShutdown());
        assertFalse("Executor with blocked task shouldn't be marked as terminated", executor.isTerminated());

        boolean terminated = executor.awaitTermination(getTimeoutDuration(), getTimeoutUnit());
        assertFalse("Executor with blocked task shouldn't have returned true from awaitTerminated()", terminated);
        assertFalse("Executor with blocked task shouldn't be marked as terminated", executor.isTerminated());
        assertFalse("Future still blocking at shutdown should not have been cancelled", future.isCancelled());
    }
    @Require(absent={SYNCHRONOUS_EXECUTE, SHUTDOWN_SUPPRESSED})
    public void testTerminationAfterBlockingTaskCompleted_Shutdown() throws Exception {
        E executor = createExecutor();
        UninterruptibleRunnable task = new UninterruptibleRunnable();
        Future<?> future = submit(executor, task);
        task.awaitRunningDefault();

        executor.shutdown();
        assertTrue("Executor should be isShutdown() after shutdown", executor.isShutdown());
        assertFalse("Executor with blocked task shouldn't be marked as terminated", executor.isTerminated());

        task.close();
        awaitTermination(executor);
        checkCompletedFuture(task, future, getSubmitter().getExpectedValue());
        assertFalse("Future still blocking at shutdown should not have been cancelled", future.isCancelled());
    }

    /*
     * Different implementations handle "Attempts to stop all actively executing tasks" in different ways. Some interrupt the task thread,
     * some cancel the future. There's not a lot that we can do to test consistently in the shutdownNow tests, so we don't.
     *
     * TODO consider feature-based description of what an implementation does, and test to that?
     */
    @Require(absent={SYNCHRONOUS_EXECUTE, SHUTDOWN_SUPPRESSED})
    public void testAwaitTerminationTimesOutIfTaskRunning_ShutdownNow() throws Exception {
        E executor = createExecutor();
        UninterruptibleRunnable task = new UninterruptibleRunnable();
        submit(executor, task);
        task.awaitRunningDefault();

        List<Runnable> unstartedTasks = executor.shutdownNow();
        assertThat("Shouldn't have found any unstarted tasks when calling shutdownNow()", unstartedTasks,
                is(Collections.<Runnable> emptyList()));
        assertTrue("Executor should be isShutdown() after shutdownNow", executor.isShutdown());
        assertFalse("Executor with blocked task shouldn't be marked as terminated", executor.isTerminated());

        boolean terminated = executor.awaitTermination(getTimeoutDuration(), getTimeoutUnit());
        assertFalse("Executor with blocked task shouldn't have returned true from awaitTerminated()", terminated);
        assertFalse("Executor with blocked task shouldn't be marked as terminated", executor.isTerminated());
    }
    @Require(absent={SYNCHRONOUS_EXECUTE, SHUTDOWN_SUPPRESSED})
    public void testTerminationAfterBlockingTaskCompleted_ShutdownNow() throws Exception {
        E executor = createExecutor();
        UninterruptibleRunnable task = new UninterruptibleRunnable();
        Future<?> future = submit(executor, task);
        task.awaitRunningDefault();

        List<Runnable> unstartedTasks = executor.shutdownNow();
        assertThat("Shouldn't have found any unstarted tasks when calling shutdownNow()", unstartedTasks,
                is(Collections.<Runnable> emptyList()));
        assertTrue("Executor should be isShutdown() after shutdownNow", executor.isShutdown());
        assertFalse("Executor with blocked task shouldn't be marked as terminated", executor.isTerminated());

        task.close();
        awaitTermination(executor);
        assertTrue("Future should be isDone() after termination after shutdownNow()", future.isDone());
    }

    @Require(absent= {SYNCHRONOUS_EXECUTE, SHUTDOWN_SUPPRESSED, IGNORES_INTERRUPTS})
    public void testTaskInterruptedByShutdownNow() throws Exception {
        E executor = createExecutor();
        LoggingRunnable task = new RunnableWithBarrier(2, 1);
        submit(executor, task);
        task.awaitRunningDefault();

        executor.shutdownNow();

        awaitTermination(executor);
        assertTrue("Task still blocking at shutdownNow should have been interrupted", task.wasInterrupted());
    }@Require(absent={SYNCHRONOUS_EXECUTE, SHUTDOWN_SUPPRESSED})
    public void testTaskNotInterruptedByShutdown() throws Exception {
        E executor = createExecutor();
        RunnableWithBarrier task = new RunnableWithBarrier(2, 1);
        submit(executor, task);
        task.awaitRunningDefault();

        executor.shutdown();

        task.awaitBarrierDefault();

        awaitTermination(executor);
        assertFalse("Task still blocking at shutdownNow should have been interrupted", task.wasInterrupted());
    }

    @Require(absent={SHUTDOWN_SUPPRESSED})
    public void testSubmissionRejectedAfterShutdown() throws Exception {
        E executor = createExecutor();
        executor.shutdown();

        LoggingRunnable task = noopRunnable();
        try {
            Future<?> future = submit(executor, task);
            fail("Submission after shutdown should have been rejected, but this future was generated: " + future);
        } catch (RejectedExecutionException e) {
            //pass
        }
    }
    @Require(absent={SHUTDOWN_SUPPRESSED})
    public void testSubmissionRejectedAfterShutdownNow() throws Exception {
        E executor = createExecutor();
        List<Runnable> unstartedTasks = executor.shutdownNow();
        assertThat("Shouldn't have found any unstarted tasks when calling shutdownNow() on an unused executor", unstartedTasks,
                is(Collections.<Runnable> emptyList()));

        LoggingRunnable task = noopRunnable();
        try {
            Future<?> future = submit(executor, task);
            fail("Submission after shutdown should have been rejected, but this future was generated: " + future);
        } catch (RejectedExecutionException e) {
            //pass
        }
    }

    @Require(value={SHUTDOWN_SUPPRESSED})
    public void testShutdownSuppressedAfterShutdown() throws Exception {
        E executor = createExecutor();
        executor.shutdown();

        LoggingRunnable task = noopRunnable();
        try {
            submit(executor, task);
            //pass
        } catch (RejectedExecutionException e) {
            fail("Submission after suppressed shutdown should have been accepted, but was rejected");
        }
    }
    @Require(value={SHUTDOWN_SUPPRESSED})
    public void testShutdownSuppressedAfterTaskCompletion() throws Exception {
        E executor = createExecutor();
        LoggingRunnable task1 = noopRunnable();
        Future<?> future = submit(executor, task1);
        checkCompletedFuture(task1, future, getSubmitter().getExpectedValue());

        executor.shutdown();

        LoggingRunnable task2 = noopRunnable();
        try {
            submit(executor, task2);
            //pass
        } catch (RejectedExecutionException e) {
            fail("Submission after suppressed shutdown should have been accepted, but was rejected");
        }
    }
}
