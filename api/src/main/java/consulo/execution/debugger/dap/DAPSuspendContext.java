package consulo.execution.debugger.dap;

import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debugger.dap.protocol.StackFrame;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPSuspendContext extends XSuspendContext {
    private DAPExecutionStack myStack;

    public DAPSuspendContext(StackFrame[] stackTraces, int threadId) {
        myStack = new DAPExecutionStack(threadId, stackTraces);
    }

    @Nullable
    @Override
    public XExecutionStack getActiveExecutionStack() {
        return myStack;
    }

    @Override
    public XExecutionStack[] getExecutionStacks() {
        return new XExecutionStack[]{myStack};
    }
}
