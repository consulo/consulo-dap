package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class EvaluateResult {
    public String result;
    public String type;
    public VariablePresentationHint presentationHint;
    public int variablesReference;
    public Integer namedVariables;
    public Integer indexedVariables;
    public String memoryReference;
}
