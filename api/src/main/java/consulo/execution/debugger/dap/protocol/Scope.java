package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class Scope {
    /**
     * Name of the scope such as 'Arguments', 'Locals', or 'Registers'. This
     * string is shown in the UI as is and can be translated.
     */
    public String name;
    /**
     * A hint for how to present this scope in the UI. If this attribute is
     * missing, the scope is shown with a generic UI.
     * Values:
     * 'arguments': Scope contains method arguments.
     * 'locals': Scope contains local variables.
     * 'registers': Scope contains registers. Only a single `registers` scope
     * should be returned from a `scopes` request.
     * 'returnValue': Scope contains one or more return values.
     * etc.
     */
    public String presentationHint;

    /**
     * The variables of this scope can be retrieved by passing the value of
     * `variablesReference` to the `variables` request as long as execution
     * remains suspended. See 'Lifetime of Object References' in the Overview
     * section for details.
     */
    public int variablesReference;
}
