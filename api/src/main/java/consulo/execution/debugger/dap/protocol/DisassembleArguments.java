package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class DisassembleArguments {
    public String memoryReference;
    public Integer offset;
    public Integer instructionOffset;
    public int instructionCount;
    public Boolean resolveSymbols;

    public DisassembleArguments(String memoryReference, int instructionCount) {
        this.memoryReference = memoryReference;
        this.instructionCount = instructionCount;
    }
}
