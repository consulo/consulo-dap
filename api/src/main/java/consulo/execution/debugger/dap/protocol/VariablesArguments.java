package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class VariablesArguments {
    /**
     * The variable for which to retrieve its children. The `variablesReference`
     * must have been obtained in the current suspended state. See 'Lifetime of
     * Object References' in the Overview section for details.
     */
    public int variablesReference;
    
    /**
     * Filter to limit the child variables to either named or indexed. If omitted,
     * both types are fetched.
     * Values: 'indexed', 'named'
     */
    public String filter;

    /**
     * The index of the first variable to return; if omitted children start at 0.
     * The attribute is only honored by a debug adapter if the corresponding
     * capability `supportsVariablePaging` is true.
     */
    public Integer start;

    /**
     * The number of variables to return. If count is missing or 0, all variables
     * are returned.
     * The attribute is only honored by a debug adapter if the corresponding
     * capability `supportsVariablePaging` is true.
     */
    public Integer count;

    /**
     * Specifies details on how to format the Variable values.
     * The attribute is only honored by a debug adapter if the corresponding
     * capability `supportsValueFormattingOptions` is true.
     */
    public Object format;

    public VariablesArguments(int variablesReference) {
        this.variablesReference = variablesReference;
    }
}
