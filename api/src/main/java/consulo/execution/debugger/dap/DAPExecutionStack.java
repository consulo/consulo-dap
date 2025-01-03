package consulo.execution.debugger.dap;

import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debugger.dap.protocol.StackFrame;
import consulo.execution.debugger.dap.protocol.Thread;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPExecutionStack extends XExecutionStack {
    private final DAPStackFrame[] myStackTraces;

    private boolean myFilled;

    private int myThreadId;

    public DAPExecutionStack(DAPContext context, Thread thread, StackFrame[] stackTraces) {
        super(thread.name);

        myThreadId = thread.id;

        myStackTraces = new DAPStackFrame[stackTraces.length];

        for (int i = 0; i < stackTraces.length; i++) {
            myStackTraces[i] = new DAPStackFrame(context, stackTraces[i]);
        }
    }

    public int getThreadId() {
        return myThreadId;
    }

    @Nullable
    @Override
    public XStackFrame getTopFrame() {
        return myStackTraces.length > 0 ? myStackTraces[0] : null;
    }

    @Override
    public void computeStackFrames(XStackFrameContainer container) {
        if (myFilled) {
            return;
        }

        myFilled = true;
        container.addStackFrames(List.of(myStackTraces), true);
    }
}
