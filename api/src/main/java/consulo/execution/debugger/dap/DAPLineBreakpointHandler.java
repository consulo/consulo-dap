package consulo.execution.debugger.dap;

import consulo.execution.debug.breakpoint.XBreakpointHandler;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
class DAPLineBreakpointHandler extends XBreakpointHandler<XLineBreakpoint<?>> {
    @Nonnull
    private final DAPDebugProcess myProcess;

    @SuppressWarnings("unchecked")
    public DAPLineBreakpointHandler(@Nonnull Class breakpointTypeClass, @Nonnull DAPDebugProcess process) {
        super(breakpointTypeClass);
        myProcess = process;
    }

    @Override
    public void registerBreakpoint(@Nonnull XLineBreakpoint<?> lineBreakpoint) {
        myProcess.updateBreakpoints(lineBreakpoint, false);
    }

    @Override
    public void unregisterBreakpoint(@Nonnull XLineBreakpoint<?> lineBreakpoint, boolean temporary) {
        myProcess.updateBreakpoints(lineBreakpoint, true);
    }
}
