package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class FunctionBreakpoint {
    public String name;
    public String condition;
    public String hitCondition;

    public FunctionBreakpoint(String name) {
        this.name = name;
    }
}
