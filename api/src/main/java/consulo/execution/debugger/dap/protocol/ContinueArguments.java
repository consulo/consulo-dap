package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public class ContinueArguments {
    /**
     * Specifies the active thread. If the debug adapter supports single thread
     * execution (see `supportsSingleThreadExecutionRequests`) and the argument
     * `singleThread` is true, only the thread with this ID is resumed.
     */
    public int threadId;

    /**
     * If this flag is true, execution is resumed only for the thread with given
     * `threadId`.
     */
    public Boolean singleThread;
}
