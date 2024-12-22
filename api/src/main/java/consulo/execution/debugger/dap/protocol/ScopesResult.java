package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class ScopesResult {
    /**
     * The scopes of the stack frame. If the array has length zero, there are no
     * scopes available.
     */
    public Scope[] scopes;
}
