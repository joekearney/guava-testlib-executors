package libjoe.testlib.executors.testers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.LoggingRunnable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

@Require(value = ExecutorFeature.LISTENING)
public class ListenableFutureTester<E extends ListeningExecutorService> extends AbstractOneSubmitterExecutorTester<E> {
    public void testListenableFutureExecuted_ControlsFactory() throws Exception {
        // make stuff
        ExecutorService ancilliaryExecutor = newAncilliarySingleThreadedExecutor();
        LoggingRunnable listener = noopRunnable();
        LoggingRunnable originalTask = noopRunnable();

        // run stuff
        ListenableFuture<?> future = (ListenableFuture<?>) submit(getSubjectGenerator().createTestSubject(), originalTask);
        future.addListener(listener, ancilliaryExecutor);

        // check stuff ran
        checkTaskRan(listener);
        checkCompletedFuture(originalTask, future, getSubmitter().getExpectedValue());

        // check stuff ran in the right place
        Executor listenerExecutor = getSubjectGenerator().getExecutorForThread(listener.getRunningThread());
        assertThat(listenerExecutor, is(sameInstance((Executor) ancilliaryExecutor)));
    }
}
