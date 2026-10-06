package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class NextArguments {
    public int threadId;
    public Boolean singleThread;
    public String granularity;

    public NextArguments(int threadId) {
        this.threadId = threadId;
    }
}
