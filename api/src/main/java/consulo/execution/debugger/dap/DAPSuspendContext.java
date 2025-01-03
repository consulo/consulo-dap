package consulo.execution.debugger.dap;

import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.StackTraceArguments;
import consulo.execution.debugger.dap.protocol.StackTraceResult;
import consulo.execution.debugger.dap.protocol.Thread;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPSuspendContext extends XSuspendContext {
    private final DAPExecutionStack[] myStacks;

    private DAPExecutionStack myActiveStack;

    public DAPSuspendContext(DAP dap, List<Thread> threads, int activeThreadId) throws InterruptedException, ExecutionException {
        myStacks = new DAPExecutionStack[threads.size()];

        for (int i = 0; i < threads.size(); i++) {
            Thread thread = threads.get(i);

            StackTraceArguments arguments = new StackTraceArguments();
            arguments.threadId = thread.id;

            StackTraceResult traceResult = dap.stackTrace(arguments).get();

            DAPExecutionStack executionStack = new DAPExecutionStack(dap, thread, traceResult.stackFrames);

            myStacks[i] = executionStack;

            if (thread.id == activeThreadId) {
                myActiveStack = executionStack;
            }
        }
    }

    @Nullable
    @Override
    public XExecutionStack getActiveExecutionStack() {
        return myActiveStack;
    }

    @Override
    public XExecutionStack[] getExecutionStacks() {
        return myStacks;
    }
}
