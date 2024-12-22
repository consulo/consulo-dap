package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class ScopesArguments {
    /**
     * Retrieve the scopes for the stack frame identified by `frameId`. The
     * `frameId` must have been obtained in the current suspended state. See
     * 'Lifetime of Object References' in the Overview section for details.
     */
    public int frameId;

    public ScopesArguments(int frameId) {
        this.frameId = frameId;
    }
}
