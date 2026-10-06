package consulo.execution.debugger.dap;

import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debugger.dap.protocol.StackFrame;
import consulo.execution.debugger.dap.protocol.StackTraceArguments;
import consulo.execution.debugger.dap.protocol.Thread;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPExecutionStack extends XExecutionStack {
    private final DAPContext myContext;
    private final int myThreadId;
    private final @Nullable DAPStackFrame[] myStackFrames;

    public DAPExecutionStack(DAPContext context, Thread thread, @Nullable StackFrame[] stackFrames) {
        super(thread.name == null ? "Thread " + thread.id : thread.name);
        myContext = context;
        myThreadId = thread.id;
        myStackFrames = stackFrames == null ? null : toFrames(context, stackFrames);
    }

    public int getThreadId() {
        return myThreadId;
    }

    @Nullable
    @Override
    public XStackFrame getTopFrame() {
        return myStackFrames != null && myStackFrames.length > 0 ? myStackFrames[0] : null;
    }

    @Override
    public void computeStackFrames(XStackFrameContainer container) {
        DAPStackFrame[] stackFrames = myStackFrames;
        if (stackFrames != null) {
            container.addStackFrames(List.of(stackFrames), true);
            return;
        }

        StackTraceArguments arguments = new StackTraceArguments();
        arguments.threadId = myThreadId;
        myContext.dap().stackTrace(arguments).whenComplete((result, error) -> {
            if (container.isObsolete()) {
                return;
            }
            if (error != null) {
                container.errorOccurred(DAPDebugProcess.errorMessage(error));
                return;
            }
            StackFrame[] frames = result.stackFrames == null ? new StackFrame[0] : result.stackFrames;
            container.addStackFrames(List.of(toFrames(myContext, frames)), true);
        });
    }

    private static DAPStackFrame[] toFrames(DAPContext context, StackFrame[] stackFrames) {
        List<DAPStackFrame> frames = new ArrayList<>(stackFrames.length);
        for (StackFrame stackFrame : stackFrames) {
            frames.add(new DAPStackFrame(context, stackFrame));
        }
        return frames.toArray(DAPStackFrame[]::new);
    }
}
