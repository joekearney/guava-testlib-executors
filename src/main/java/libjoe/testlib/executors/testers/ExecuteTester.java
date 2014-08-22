package libjoe.testlib.executors.testers;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.ExecutorSubmitter;
import libjoe.testlib.executors.ExecutorTestSubjectGenerator;
import libjoe.testlib.executors.LoggingRunnable;

public class ExecuteTester<E extends Executor> extends AbstractExecutorTester<E, ExecutorTestSubjectGenerator<E>> {
    public void testExecuteSingleTaskExecutes() throws Exception {
        LoggingRunnable task = noopRunnable();
        E executor = createExecutor();
        executor.execute(task);
        checkTaskRan(task);
    }

    /*
     * Tests asserting that execution of a runnable that throws doesn't kill the executor - subsequent tasks should still get queued.
     */
    @Require(absent = ExecutorFeature.SYNCHRONOUS_EXCEPTIONS)
    public void testExecuteThrowingTaskAllowsSubsequentExecute() throws Exception {
        E executor = createExecutor();

        LoggingRunnable throwingRunnable = throwingRunnable();
        LoggingRunnable anotherTask = noopRunnable();

        executor.execute(throwingRunnable);
        checkTaskRan(throwingRunnable);

        executor.execute(anotherTask);
        checkTaskRan(anotherTask);
    }
    @Require(value = ExecutorFeature.SYNCHRONOUS_EXCEPTIONS)
    public void testExecuteThrowingTaskAllowsSubsequentExecute_synchronous() throws Exception {
        E executor = createExecutor();

        LoggingRunnable throwingRunnable = throwingRunnable();
        LoggingRunnable anotherTask = noopRunnable();

        try {
            executor.execute(throwingRunnable);
            fail("Expected throwingRunnable executed in the current to throw out to the caller, but execute() returned normally");
        } catch (RuntimeRunnableException e) {
            // pass
        }
        checkTaskRan(throwingRunnable);

        executor.execute(anotherTask);
        checkTaskRan(anotherTask);
    }

    @Require(value = ExecutorFeature.REJECTS_EXCESS_TASKS)
    public void testExcessTasksRejected() throws Exception {
        E executor = createExecutor();

        addTasksToCapacity(executor, ExecutorSubmitter.EXECUTE);

        // now add one more to the queue, and expect it to pop
        try {
            executor.execute(noopRunnable());
            fail("Expected RejectedExecutionException after submitting a new task for execution beyond the capacity of the executor");
        } catch (RejectedExecutionException e) {
            // expected
        }
    }
    public void testExecuteNullPointerExceptions() throws Exception {
		runNullPointerTests("execute");
	}
}
