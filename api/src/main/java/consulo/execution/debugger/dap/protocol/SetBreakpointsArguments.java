package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class SetBreakpointsArguments {
    public Source source;

    public SourceBreakpoint[] breakpoints;

    public int[] lines;

    public boolean sourceModified;
}
