package libjoe.testlib.executors;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface LoggingRunnable extends Runnable {
    Callable<Object> asCallableReturningDefault();
    <T> Callable<T> asCallableReturningValue(T result);
    Callable<Object> asCallableReturningNothing();
    boolean wasInterrupted();
    boolean wasRun();
    boolean hasFinished();
    boolean awaitRun(long timeout, TimeUnit unit) throws InterruptedException;
    boolean awaitRunDefault() throws InterruptedException;
    Thread getRunningThread();
}