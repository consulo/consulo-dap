package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class ReadMemoryArguments {
    public String memoryReference;
    public Integer offset;
    public int count;

    public ReadMemoryArguments(String memoryReference, int count) {
        this.memoryReference = memoryReference;
        this.count = count;
    }
}
