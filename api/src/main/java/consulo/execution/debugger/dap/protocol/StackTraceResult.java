package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class StackTraceResult {
    /**
     * The frames of the stack frame. If the array has length zero, there are no
     * stack frames available.
     * This means that there is no location information available.
     */
    public StackFrame[] stackFrames;

    /**
     * The total number of frames available in the stack. If omitted or if
     * `totalFrames` is larger than the available frames, a client is expected
     * to request frames until a request returns less frames than requested
     * (which indicates the end of the stack). Returning monotonically
     * increasing `totalFrames` values for subsequent requests can be used to
     * enforce paging in the client.
     */
    public Integer totalFrames;
}
