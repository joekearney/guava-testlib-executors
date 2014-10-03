package libjoe.testlib.executors.testers;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.ExecutorTestSubjectGenerator;

/**
 * Tests for shutdown basics with no task submission.
 *
 * @author Joe Kearney
 */
@Require(value=ExecutorFeature.EXECUTOR_SERVICE, absent=ExecutorFeature.SHUTDOWN_SUPPRESSED)
public class BasicShutdownTester<E extends ExecutorService> extends AbstractExecutorTester<E, ExecutorTestSubjectGenerator<E>> {
    public void testShutdown_NoTasks() throws Exception {
        E executor = createExecutor();
        assertFalse("Executor should not be isShutdown() before shutdown() on empty executor", executor.isShutdown());
        assertFalse("Executor should not be isTerminated() before shutdown() on empty executor", executor.isTerminated());
        executor.shutdown();
        awaitTermination(executor);
        assertThat("Executor should be isShutdown() after shutdown() on empty executor", executor.isShutdown());
        assertThat("Executor should be isTerminated() after shutdown() on empty executor", executor.isTerminated());
    }
    public void testShutdownNow_NoTasks() throws Exception {
        E executor = createExecutor();
        List<Runnable> tasksOutstanding = executor.shutdownNow();
        awaitTermination(executor);
        assertThat("Should be no tasks outstanding after shutdownNow() on empty executor", tasksOutstanding.isEmpty());
        assertThat("Executor should be isShutdown() after shutdownNow() on empty executor", executor.isShutdown());
        assertThat("Executor should be isTerminated() after shutdownNow() on empty executor", executor.isTerminated());
    }

    public void testNotShutdownAtStart() throws Exception {
        assertFalse("Executor should not be isShutdown() before doing anything to it", createExecutor().isShutdown());
    }
    public void testNotTerminatedAtStart() throws Exception {
        assertFalse("Executor should not be isTerminated() before doing anything to it", createExecutor().isTerminated());
    }
    public void testAwaitTerminationWithoutShutdown() throws Exception {
        /*
         * Just give it a positive number, don't want to sleep for too long. We're really just checking whether it returns immediately.
         */
        if (createExecutor().awaitTermination(10, TimeUnit.MILLISECONDS)) {
            fail("Executor terminated without shutdown() or shutdownNow()");
        }
        assertFalse("Executor should not be isTerminated() before doing anything to it", createExecutor().isTerminated());
        assertFalse("Executor should not be isShutdown() before doing anything to it", createExecutor().isShutdown());
    }
}
