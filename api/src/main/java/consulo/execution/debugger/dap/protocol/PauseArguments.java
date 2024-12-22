package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class PauseArguments {
    /**
     * Pause execution for this thread.
     */
    public int threadId;

    public PauseArguments(int threadId) {
        this.threadId = threadId;
    }
}
