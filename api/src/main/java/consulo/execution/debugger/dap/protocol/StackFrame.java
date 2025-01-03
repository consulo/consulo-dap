package consulo.execution.debugger.dap.protocol;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class StackFrame {
    /**
     * An identifier for the stack frame. It must be unique across all threads.
     * This id can be used to retrieve the scopes of the frame with the `scopes`
     * request or to restart the execution of a stack frame.
     */
    public int id;

    /**
     * The name of the stack frame, typically a method name.
     */
    public String name;

    /**
     * The source of the frame.
     */
    public Source source;

    /**
     * The line within the source of the frame. If the source attribute is missing
     * or doesn't exist, `line` is 0 and should be ignored by the client.
     */
    public int line;

    /**
     * Start position of the range covered by the stack frame. It is measured in
     * UTF-16 code units and the client capability `columnsStartAt1` determines
     * whether it is 0- or 1-based. If attribute `source` is missing or doesn't
     * exist, `column` is 0 and should be ignored by the client.
     */
    public int column;

    /**
     * The end line of the range covered by the stack frame.
     */
    public Integer endLine;

    /**
     * End position of the range covered by the stack frame. It is measured in
     * UTF-16 code units and the client capability `columnsStartAt1` determines
     * whether it is 0- or 1-based.
     */
    public Integer endColumn;

    /**
     * Indicates whether this frame can be restarted with the `restartFrame`
     * request. Clients should only use this if the debug adapter supports the
     * `restart` request and the corresponding capability `supportsRestartFrame`
     * is true. If a debug adapter has this capability, then `canRestart` defaults
     * to `true` if the property is absent.
     */
    public Boolean canRestart;

    /**
     * A memory reference for the current instruction pointer in this frame.
     */
    public String instructionPointerReference;

    /**
     * The module associated with this frame, if any.
     */
    public Object moduleId;

    /**
     * A hint for how to present this frame in the UI.
     * A value of `label` can be used to indicate that the frame is an artificial
     * frame that is used as a visual label or separator. A value of `subtle` can
     * be used to change the appearance of a frame in a 'subtle' way.
     * Values: 'normal', 'label', 'subtle'
     */
    public String presentationHint;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StackFrame that = (StackFrame) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
