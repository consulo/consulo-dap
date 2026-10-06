package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public final class EvaluateContext {
    public static final String WATCH = "watch";
    public static final String REPL = "repl";
    public static final String HOVER = "hover";
    public static final String CLIPBOARD = "clipboard";
    public static final String VARIABLES = "variables";

    private EvaluateContext() {
    }
}
