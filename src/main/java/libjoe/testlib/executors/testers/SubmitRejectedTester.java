package libjoe.testlib.executors.testers;

import static libjoe.testlib.executors.ExecutorFeature.EXECUTOR_SERVICE;
import static libjoe.testlib.executors.ExecutorFeature.REJECTS_EXCESS_TASKS;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS_EXECUTE;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS_TASK_START;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.ExecutorServiceSubmitters;

@Require(value = { EXECUTOR_SERVICE })
public class SubmitRejectedTester<E extends ExecutorService> extends AbstractOneSubmitterExecutorTester<E> {
    @Require(value = { REJECTS_EXCESS_TASKS }, absent = { SYNCHRONOUS_EXECUTE })
    public void testExcessTasksRejected_Execute() throws Exception {
        E executor = createExecutor();

        addTasksToCapacity(executor);

        // now add one more to the queue, and expect it to pop
        try {
            executor.execute(noopRunnable());
            fail("Expected RejectedExecutionException after submitting a new task for execution beyond the capacity of the executor");
        } catch (RejectedExecutionException e) {
            // expected
        }
    }
    /*
     * Does the executor still behave when it gets a huge number of tasks submitted?
     *
     * absent=SYNCHRONOUS_TASK_START because we don't want a newCachedThreadPool to start thousands of threads simultaneously.
     */
    private static final int MANY_TASKS = 10_000;
    @Require(absent = { REJECTS_EXCESS_TASKS, SYNCHRONOUS_EXECUTE, SYNCHRONOUS_TASK_START })
    public void testMassiveExcessTasksNotRejected() throws Exception {
        final List<RunnableWithBarrier> tasks = new ArrayList<>();
        E executor = createExecutor();

        try {
            for (int i = 0; i < MANY_TASKS; i++) {
                RunnableWithBarrier task = new RunnableWithBarrier(2, 1);
                tasks.add(task);
                submit(executor, task);
            }

            // we managed to add many tasks
        } catch (RejectedExecutionException e) {
            // anything else causes an error rather than a failure
            fail("Rejected execution on the " + (tasks.size() + 1) + "th submission", e);
        } finally {
            // be nice, clean up
            executor.shutdownNow();
            for (RunnableWithBarrier task : tasks) {
                task.resetBarrier();
            }
            // try to avoid causing timeouts in later tests
            System.gc();
        }
    }
    @Require(absent = { REJECTS_EXCESS_TASKS, SYNCHRONOUS_TASK_START })
    @ExecutorServiceSubmitters.Require(value=ExecutorServiceSubmitters.INVOKE_ALL)
    public void testMassiveExcessTasksNotRejected_InvokeAll() throws Exception {
        /*
         * This differs from the above in that all of the tasks are submitted in a batch.
         */
        final List<RunnableWithBarrier> tasks = new ArrayList<>();
        final List<Callable<Object>> callables = new ArrayList<>();
        E executor = createExecutor();

        try {
            for (int i = 0; i < MANY_TASKS; i++) {
                RunnableWithBarrier task = new RunnableWithBarrier(2, 1);
                tasks.add(task);
                callables.add(task.asCallableReturningDefault());
            }
            executor.invokeAll(callables, getTimeoutDuration(), getTimeoutUnit());

            // we managed to add many tasks, we don't care how many actually get executed
        } catch (RejectedExecutionException e) {
            // anything else causes an error rather than a failure
            fail("Rejected execution on the " + (callables.size() + 1) + "th submission", e);
        } finally {
            // be nice, clean up
            executor.shutdownNow();
            for (RunnableWithBarrier task : tasks) {
                task.resetBarrier();
            }
            // try to avoid causing timeouts in later tests
            System.gc();
        }
    }
}
