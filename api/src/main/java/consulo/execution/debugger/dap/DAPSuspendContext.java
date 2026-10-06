package consulo.execution.debugger.dap;

import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debugger.dap.protocol.StackFrame;
import consulo.execution.debugger.dap.protocol.Thread;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPSuspendContext extends XSuspendContext {
    private final DAPExecutionStack[] myStacks;
    private DAPExecutionStack myActiveStack;

    public DAPSuspendContext(DAPContext context, List<Thread> threads, int activeThreadId, @Nullable StackFrame[] activeFrames) {
        myStacks = new DAPExecutionStack[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            Thread thread = threads.get(i);
            boolean active = thread.id == activeThreadId;
            DAPExecutionStack executionStack = new DAPExecutionStack(context, thread, active ? activeFrames : null);
            myStacks[i] = executionStack;
            if (active) {
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
