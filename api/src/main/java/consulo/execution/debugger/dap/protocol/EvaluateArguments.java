package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class EvaluateArguments {
    public String expression;
    public Integer frameId;
    public String context;

    public EvaluateArguments(String expression, Integer frameId, String context) {
        this.expression = expression;
        this.frameId = frameId;
        this.context = context;
    }
}
