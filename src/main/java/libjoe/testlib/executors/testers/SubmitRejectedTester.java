package libjoe.testlib.executors.testers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;

@Require(value = { ExecutorFeature.REJECTS_EXCESS_TASKS, ExecutorFeature.EXECUTOR_SERVICE })
public class SubmitRejectedTester<E extends ExecutorService> extends AbstractExecutorTester<E> {
    public void testExcessTasksRejected_Execute() throws Exception {
        doTestExcessTasksRejected(ExecutorSubmitters.EXECUTE);
    }
    public void testExcessTasksRejected_Runnable() throws Exception {
        doTestExcessTasksRejected(ExecutorSubmitters.RUNNABLE);
    }
    public void testExcessTasksRejected_RunnableWithValue() throws Exception {
        doTestExcessTasksRejected(ExecutorSubmitters.RUNNABLE_WITH_VALUE);
    }
    public void testExcessTasksRejected_Callable() throws Exception {
        doTestExcessTasksRejected(ExecutorSubmitters.CALLABLE);
    }
    
    private void doTestExcessTasksRejected(ExecutorSubmitter submitter) throws Exception {
        E executor = getSubjectGenerator().createTestSubject();

        addTasksToCapacity(executor, submitter);

        // now add one more to the queue, and expect it to pop
        try {
            executor.execute(noopRunnable());
            fail("Expected RejectedExecutionException after submitting a new task for execution beyond the capacity of the executor");
        } catch (RejectedExecutionException e) {
            // expected
        }
    }
}
