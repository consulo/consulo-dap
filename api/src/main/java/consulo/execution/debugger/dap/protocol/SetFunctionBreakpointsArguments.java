package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class SetFunctionBreakpointsArguments {
    public FunctionBreakpoint[] breakpoints;

    public SetFunctionBreakpointsArguments(FunctionBreakpoint[] breakpoints) {
        this.breakpoints = breakpoints;
    }
}
