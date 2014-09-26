package libjoe.testlib.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public interface LoggingRunnable extends Runnable {
    Callable<Object> asCallableReturningDefault();
    <T> Callable<T> asCallableReturningValue(T result);
    Callable<Object> asCallableReturningNothing();
    boolean wasInterrupted();
    boolean wasRun();
    boolean hasFinished();
    /**
     * Wait a short time until this task has started running.
     */
    void awaitRunningDefault() throws InterruptedException, TimeoutException;
    Thread getRunningThread();
}