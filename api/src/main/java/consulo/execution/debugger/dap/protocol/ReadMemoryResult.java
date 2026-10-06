package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class ReadMemoryResult {
    public String address;
    public Integer unreadableBytes;
    public String data;
}
