package consulo.execution.debugger.dap.protocol.event;

import consulo.execution.debugger.dap.protocol.Event;
import consulo.execution.debugger.dap.protocol.Source;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
@Event("output")
public class OutputEvent implements Supplier<OutputEvent> {
    /**
     * The output category. If not specified or if the category is not
     * understood by the client, `console` is assumed.
     * Values:
     * 'console': Show the output in the client's default message UI, e.g. a
     * 'debug console'. This category should only be used for informational
     * output from the debugger (as opposed to the debuggee).
     * 'important': A hint for the client to show the output in the client's UI
     * for important and highly visible information, e.g. as a popup
     * notification. This category should only be used for important messages
     * from the debugger (as opposed to the debuggee). Since this category value
     * is a hint, clients might ignore the hint and assume the `console`
     * category.
     * 'stdout': Show the output as normal program output from the debuggee.
     * 'stderr': Show the output as error program output from the debuggee.
     * 'telemetry': Send the output to telemetry instead of showing it to the
     * user.
     * etc.
     */
    public String category;

    /**
     * The output to report.
     *
     * ANSI escape sequences may be used to influence text color and styling if
     * `supportsANSIStyling` is present in both the adapter's `Capabilities` and
     * the client's `InitializeRequestArguments`. A client may strip any
     * unrecognized ANSI sequences.
     *
     * If the `supportsANSIStyling` capabilities are not both true, then the
     * client should display the output literally.
     */
    public String output;

    /**
     * Support for keeping an output log organized by grouping related messages.
     * Values:
     * 'start': Start a new group in expanded mode. Subsequent output events are
     * members of the group and should be shown indented.
     * The `output` attribute becomes the name of the group and is not indented.
     * 'startCollapsed': Start a new group in collapsed mode. Subsequent output
     * events are members of the group and should be shown indented (as soon as
     * the group is expanded).
     * The `output` attribute becomes the name of the group and is not indented.
     * 'end': End the current group and decrease the indentation of subsequent
     * output events.
     * A non-empty `output` attribute is shown as the unindented end of the
     * group.
     */
    public String group;

    /**
     * If an attribute `variablesReference` exists and its value is > 0, the
     * output contains objects which can be retrieved by passing
     * `variablesReference` to the `variables` request as long as execution
     * remains suspended. See 'Lifetime of Object References' in the Overview
     * section for details.
     */
    public Number variablesReference;

    /**
     * The source location where the output was produced.
     */
    public Source source;

    /**
     * The source location's line where the output was produced.
     */
    public Integer line;

    /**
     * The position in `line` where the output was produced. It is measured in
     * UTF-16 code units and the client capability `columnsStartAt1` determines
     * whether it is 0- or 1-based.
     */
    public Integer column;

    /**
     * Additional data to report. For the `telemetry` category the data is sent
     * to telemetry, for the other categories the data is shown in JSON format.
     */
    public Object data;

    /**
     * A reference that allows the client to request the location where the new
     * value is declared. For example, if the logged value is function pointer,
     * the adapter may be able to look up the function's location. This should
     * be present only if the adapter is likely to be able to resolve the
     * location.
     *
     * This reference shares the same lifetime as the `variablesReference`. See
     * 'Lifetime of Object References' in the Overview section for details.
     */
    public Number locationReference;

    @Override
    public OutputEvent get() {
        return this;
    }

    @Override
    public String toString() {
        return "OutputEvent{" +
            "category='" + category + '\'' +
            ", output='" + output + '\'' +
            ", group='" + group + '\'' +
            ", variablesReference=" + variablesReference +
            ", source=" + source +
            ", line=" + line +
            ", column=" + column +
            ", data=" + data +
            ", locationReference=" + locationReference +
            '}';
    }
}
