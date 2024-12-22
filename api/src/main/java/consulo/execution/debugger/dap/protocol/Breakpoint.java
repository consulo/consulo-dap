package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class Breakpoint {
    /**
     * The identifier for the breakpoint. It is needed if breakpoint events are
     * used to update or remove breakpoints.
     */
    public Integer id;
    
    /**
     * If true, the breakpoint could be set (but not necessarily at the desired
     * location).
     */
    public boolean verified;

    /**
     * A message about the state of the breakpoint.
     * This is shown to the user and can be used to explain why a breakpoint could
     * not be verified.
     */
    public String message;

    /**
     * The source where the breakpoint is located.
     */
    public Source source;

    /**
     * The start line of the actual range covered by the breakpoint.
     */
    public Integer line;

    /**
     * Start position of the source range covered by the breakpoint. It is
     * measured in UTF-16 code units and the client capability `columnsStartAt1`
     * determines whether it is 0- or 1-based.
     */
    public Integer column;

    /**
     * The end line of the actual range covered by the breakpoint.
     */
    public Integer endLine;

    /**
     * End position of the source range covered by the breakpoint. It is measured
     * in UTF-16 code units and the client capability `columnsStartAt1` determines
     * whether it is 0- or 1-based.
     * If no end line is given, then the end column is assumed to be in the start
     * line.
     */
    public Integer endColumn;

    /**
     * A memory reference to where the breakpoint is set.
     */
    public String instructionReference;

    /**
     * The offset from the instruction reference.
     * This can be negative.
     */
    public Long offset;

    /**
     * A machine-readable explanation of why a breakpoint may not be verified. If
     * a breakpoint is verified or a specific reason is not known, the adapter
     * should omit this property. Possible values include:
     * <p>
     * - `pending`: Indicates a breakpoint might be verified in the future, but
     * the adapter cannot verify it in the current state.
     * - `failed`: Indicates a breakpoint was not able to be verified, and the
     * adapter does not believe it can be verified without intervention.
     * Values: 'pending', 'failed'
     */
    public String reason;
}
