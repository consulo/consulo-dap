package consulo.execution.debugger.dap;

import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debugger.dap.protocol.StackFrame;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPExecutionStack extends XExecutionStack {
    private final DAPStackFrame[] myStackTraces;

    public DAPExecutionStack(int threadId, StackFrame[] stackTraces) {
        super(String.valueOf(threadId));

        myStackTraces = new DAPStackFrame[stackTraces.length];

        for (int i = 0; i < stackTraces.length; i++) {
            myStackTraces[i] = new DAPStackFrame(stackTraces[i]);
        }
    }

    @Nullable
    @Override
    public XStackFrame getTopFrame() {
        return myStackTraces.length > 0 ? myStackTraces[0] : null;
    }

    @Override
    public void computeStackFrames(XStackFrameContainer container) {
        container.addStackFrames(List.of(myStackTraces), true);
    }
}
