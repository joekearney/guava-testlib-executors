package libjoe.testlib.executors.testers;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static libjoe.testlib.executors.ExecutorFeature.SYNCHRONOUS_EXECUTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import libjoe.testlib.executors.ExecutorFeature;
import libjoe.testlib.executors.ExecutorFeature.Require;
import libjoe.testlib.executors.ExecutorSubmitter;
import libjoe.testlib.executors.ExecutorTestSubjectGenerator;
import libjoe.testlib.executors.LoggingRunnable;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Tests for {@link ExecutorService#invokeAny}.
 *
 * @author Joe Kearney
 * @param <E>
 */
@Require(value = ExecutorFeature.EXECUTOR_SERVICE)
public class InvokeAnyTester<E extends ExecutorService> extends AbstractExecutorTester<E, ExecutorTestSubjectGenerator<E>> {
    public void testReturnsResultOfOnlyTask() throws Exception {
        LoggingRunnable task = noopRunnable();
        Object result = createExecutor().invokeAny(ImmutableList.of(task.asCallableReturningDefault()));
        assertThat("Returned value didn't match expected", result, is(sameInstance(ExecutorSubmitter.RETURN_VALUE)));
        checkTaskRan(task);
    }
    public void testReturnsResultOfOnlyCompletingTask() throws Exception {
        LoggingRunnable task = noopRunnable();
        Collection<Callable<Object>> callables = ImmutableList.of(
                throwingRunnable().asCallableReturningNothing(),
                task.asCallableReturningDefault(),
                throwingRunnable().asCallableReturningNothing());
        Object result = createExecutor().invokeAny(callables);
        assertThat("Returned value didn't match expected", result, is(sameInstance(ExecutorSubmitter.RETURN_VALUE)));
    }
    public void testRethrowsExceptionOfOnlyFailingTask() throws Exception {
        LoggingRunnable task = throwingRunnable();
        try {
            Object result = createExecutor().invokeAny(ImmutableList.of(task.asCallableReturningNothing()));
            fail("Expected exception but got result: " + result);
        } catch (ExecutionException e) {
            checkExceptionInCause(e);
            // pass
        }
    }
    public void testRethrowsExceptionOfAFailingTask() throws Exception {
        try {
            ImmutableList<Callable<Object>> tasks = ImmutableList.of(
                    throwingRunnable().asCallableReturningNothing(),
                    throwingRunnable().asCallableReturningNothing(),
                    throwingRunnable().asCallableReturningNothing());
            Object result = createExecutor().invokeAny(tasks);
            fail("Expected exception but got result: " + result);
        } catch (ExecutionException e) {
            checkExceptionInCause(e);
            // pass
        }
    }
    private void checkExceptionInCause(ExecutionException e) {
        List<Throwable> causalChain = Throwables.getCausalChain(e);
        assertThat("Causal chain did not contain expected exception. Was: " + Throwables.getStackTraceAsString(e),
                Iterables.any(causalChain, Predicates.instanceOf(RuntimeRunnableException.class)));
    }

    // IAE tests
    public void testIllegalArgumentExceptionOnEmptyList() throws Exception {
        try {
            createExecutor().invokeAny(ImmutableList.<Callable<Object>> of());
            fail("Should have thrown IllegalArgumentException on invokeAny(<empty>)");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }
    public void testIllegalArgumentExceptionOnEmptyList_Timeout() throws Exception {
        try {
            createExecutor().invokeAny(ImmutableList.<Callable<Object>> of(), getTimeoutDuration(), getTimeoutUnit());
            fail("Should have thrown IllegalArgumentException on invokeAny(<empty>)");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    // Interruption tests
    /*
     * Don't respect ExecutorFeature.IGNORES_INTERRUPTS here, which applies only to the threads created by the test thread factory.
     * Synchronous versions allow the interpretation of the spec where the caught InterruptionException is the failure that causes
     * an ExecutionException to be thrown.
     */
    @Require(value=SYNCHRONOUS_EXECUTION)
    public void testInterruptedWhileWaiting_Synchronous_NoTimeout() throws Exception {
        final RunnableWithBarrier task = new RunnableWithBarrier(2, 2);

        interruptMeAtBarrier(task);
        try {
            createExecutor().invokeAny(ImmutableList.of(task.asCallableReturningDefault()));
            fail("Interrupted while waiting on invokeAny(task), should have thrown InterruptedException or an exception with that root cause");
        } catch (InterruptedException expected) {
        } catch (ExecutionException e) {
            assertThat("Expected InterruptedException as the root cause", Throwables.getRootCause(e) instanceof InterruptedException);
        }

    }
    @Require(value=SYNCHRONOUS_EXECUTION)
    public void testInterruptedWhileWaiting_Synchronous_Timeout() throws Exception {
        final RunnableWithBarrier task = new RunnableWithBarrier(2, 2);

        interruptMeAtBarrier(task);
        try {
            createExecutor().invokeAny(ImmutableList.of(task.asCallableReturningDefault()), getTimeoutDuration(), getTimeoutUnit());
            fail("Interrupted while waiting on invokeAny(task, timeout, unit), should have thrown InterruptedException or an exception with that root cause");
        } catch (InterruptedException expected) {
        } catch (ExecutionException e) {
            assertThat("Expected InterruptedException as the root cause", Throwables.getRootCause(e) instanceof InterruptedException);
        }
    }
    @Require(absent=SYNCHRONOUS_EXECUTION)
    public void testInterruptedWhileWaiting_NoTimeout() throws Exception {
        final RunnableWithBarrier task = new RunnableWithBarrier(2, 2);

        interruptMeAtBarrier(task);
        try {
            createExecutor().invokeAny(ImmutableList.of(task.asCallableReturningDefault()));
            fail("Interrupted while waiting on invokeAny(task), should have thrown InterruptedException");
        } catch (InterruptedException expected) {}
    }
    @Require(absent=SYNCHRONOUS_EXECUTION)
    public void testInterruptedWhileWaiting_Timeout() throws Exception {
        final RunnableWithBarrier task = new RunnableWithBarrier(2, 2);

        interruptMeAtBarrier(task);
        try {
            createExecutor().invokeAny(ImmutableList.of(task.asCallableReturningDefault()), getTimeoutDuration(), getTimeoutUnit());
            fail("Interrupted while waiting on invokeAny(task, timeout, unit), should have thrown InterruptedException");
        } catch (InterruptedException expected) {}
    }

    // Timeout tests
    @Require(value=SYNCHRONOUS_EXECUTION)
    public void testLongRunningTaskTimesOut_Synchronous() throws Exception {
        /*
         * Synchronous versions allow the interpretation of the spec where the caught InterruptionException is the failure that causes an
         * ExecutionException to be thrown.
         */
        try {
            createExecutor().invokeAny(ImmutableList.of(new RunnableWithBarrier(2, 1).asCallableReturningDefault()), 10, MILLISECONDS);
            fail("invokeAny(<blocking task>) should not have returned");
        } catch (TimeoutException pass) {
        } catch (ExecutionException e) {
            assertThat("Expected TimeoutException as the root cause", Throwables.getRootCause(e) instanceof TimeoutException);
        }
    }
    @Require(absent=SYNCHRONOUS_EXECUTION)
    public void testLongRunningTaskTimesOut() throws Exception {
        try {
            createExecutor().invokeAny(ImmutableList.of(new RunnableWithBarrier(2, 1).asCallableReturningDefault()), 10, MILLISECONDS);
            fail("invokeAny(<blocking task>) should not have returned");
        } catch (TimeoutException pass) {}
    }

    // NPE tests
    public void testInvokeAnyNullPointerException_OnlyNullTask() throws Exception {
        E executor = createExecutor();
        checkThrowsNpe(executor, (Callable<Object>)null);
        checkThrowsNpe_Timeout(executor, (Callable<Object>)null);
    }
    @SuppressWarnings("unchecked")
    public void testInvokeAnyNullPointerException_NullTaskAfterPass_NoNpe() throws Exception {
        /*
         * Some implementations return the value from the passing task, some check all tasks and eagerly throw NPE. Here we allow either
         * interpretation of the spec.
         */

        E executor = createExecutor();

        try {
            assertThat("invokeAny(pass, null) should have returned a value",
                    executor.invokeAny(Lists.newArrayList(noopRunnable().asCallableReturningDefault(), null)),
                    is(sameInstance(ExecutorSubmitter.RETURN_VALUE)));
        } catch (NullPointerException e) {
            // allow
        }

        try {
            assertThat("invokeAny(pass, null) should have returned a value",
                    executor.invokeAny(Lists.newArrayList(noopRunnable().asCallableReturningDefault(), null)),
                    is(sameInstance(ExecutorSubmitter.RETURN_VALUE)));
        } catch (NullPointerException e) {
            // allow
        }
    }
    public void testInvokeAnyNullPointerException_NullTaskAfterFail() throws Exception {
        E executor = createExecutor();
        checkThrowsNpe(executor, throwingRunnable().asCallableReturningNothing(), null);
        checkThrowsNpe_Timeout(executor, throwingRunnable().asCallableReturningNothing(), null);
    }
    public void testInvokeAnyNullPointerException_NullTaskBeforePass() throws Exception {
        E executor = createExecutor();
        checkThrowsNpe(executor, null, noopRunnable().asCallableReturningDefault());
        checkThrowsNpe_Timeout(executor, null, noopRunnable().asCallableReturningDefault());
    }
    public void testInvokeAnyNullPointerException_NullTaskBeforeFail() throws Exception {
        E executor = createExecutor();
        checkThrowsNpe(executor, null, throwingRunnable().asCallableReturningNothing());
        checkThrowsNpe_Timeout(executor, null, throwingRunnable().asCallableReturningNothing());
    }
    @SafeVarargs
    private final void checkThrowsNpe(E executor, Callable<Object> ... tasks) throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<Object>> taskList = Lists.newArrayList(tasks);
        try {
            Object result = executor.invokeAny(taskList);
            fail("invokeAny(" + taskList + ") should have thrown NPE, but returned " + result);
        } catch (NullPointerException expected) {}
    }
    @SafeVarargs
    private final void checkThrowsNpe_Timeout(E executor, Callable<Object> ... tasks) throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<Object>> taskList = Lists.newArrayList(tasks);
        try {
            Object result = executor.invokeAny(taskList, getTimeoutDuration(), getTimeoutUnit());
            fail("invokeAny(" + taskList + ", timeout, unit) should have thrown NPE, but returned " + result);
        } catch (NullPointerException expected) {}
    }
    public void testInvokeAnyNullPointerExceptions() throws NoSuchMethodException, SecurityException {
        runNullPointerTests("invokeAny");
    }
}
