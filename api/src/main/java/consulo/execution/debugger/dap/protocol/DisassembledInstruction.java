package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class DisassembledInstruction {
    public String address;
    public String instructionBytes;
    public String instruction;
    public String symbol;
    public Source location;
    public Integer line;
    public Integer column;
    public Integer endLine;
    public Integer endColumn;
    public String presentationHint;
}
