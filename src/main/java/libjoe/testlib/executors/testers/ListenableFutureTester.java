package libjoe.testlib.executors.testers;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

@Require(value=ExecutorFeature.LISTENING)
public class ListenableFutureTester<E extends ListeningExecutorService> extends AbstractExecutorTester<E> {
    public void testListenableFutureExecuted_Runnable() throws Exception {
        doTestListenableFutureExecuted(ExecutorSubmitters.RUNNABLE);
    }
    public void testListenableFutureExecuted_RunnableWithValue() throws Exception {
        doTestListenableFutureExecuted(ExecutorSubmitters.RUNNABLE_WITH_VALUE);
    }
    public void testListenableFutureExecuted_Callable() throws Exception {
        doTestListenableFutureExecuted(ExecutorSubmitters.CALLABLE);
    }
    private void doTestListenableFutureExecuted(ExecutorSubmitter submitter) throws Exception {
        LoggingRunnable originalTask = noopRunnable();
        ListenableFuture<?> future = (ListenableFuture<?>) submitter.submit(getSubjectGenerator().createTestSubject(), originalTask);
        
        LoggingRunnable listener = noopRunnable();
        future.addListener(listener, sameThreadExecutor());
        checkTaskRan(listener);
        checkCompletedFuture(originalTask, future, submitter.getExpectedValue());
    }
}
