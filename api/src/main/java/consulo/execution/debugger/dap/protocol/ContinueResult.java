package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public class ContinueResult {
    /**
     * The value true (or a missing property) signals to the client that all
     * threads have been resumed. The value false indicates that not all threads
     * were resumed.
     */
    public Boolean allThreadsContinued;
}
