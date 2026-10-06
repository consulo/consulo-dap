package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class StepInArguments {
    public int threadId;
    public Boolean singleThread;
    public Integer targetId;
    public String granularity;

    public StepInArguments(int threadId) {
        this.threadId = threadId;
    }
}
