package consulo.execution.debugger.dap.protocol;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class SetBreakpointsResult {
    /**
     * Information about the breakpoints.
     * The array elements are in the same order as the elements of the
     * `breakpoints` (or the deprecated `lines`) array in the arguments.
     */
    public Breakpoint[] breakpoints;

    @Override
    public String toString() {
        return "SetBreakpointsResult{" +
            "breakpoints=" + (breakpoints == null ? null : Arrays.asList(breakpoints)) +
            '}';
    }
}
