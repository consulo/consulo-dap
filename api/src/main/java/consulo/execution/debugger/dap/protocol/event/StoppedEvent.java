package consulo.execution.debugger.dap.protocol.event;

import consulo.execution.debugger.dap.protocol.Event;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
@Event("stopped")
public class StoppedEvent implements Supplier<StoppedEvent> {
    /**
     * The reason for the event.
     * For backward compatibility this string is shown in the UI if the
     * `description` attribute is missing (but it must not be translated).
     * Values: 'step', 'breakpoint', 'exception', 'pause', 'entry', 'goto',
     * 'function breakpoint', 'data breakpoint', 'instruction breakpoint', etc.
     */
    public String reason;

    /**
     * The full reason for the event, e.g. 'Paused on exception'. This string is
     * shown in the UI as is and can be translated.
     */
    public String description;

    /**
     * The thread which was stopped.
     */
    public Integer threadId;

    /**
     * A value of true hints to the client that this event should not change the
     * focus.
     */
    public Boolean preserveFocusHint;

    /**
     * Additional information. E.g. if reason is `exception`, text contains the
     * exception name. This string is shown in the UI.
     */
    public String text;

    /**
     * If `allThreadsStopped` is true, a debug adapter can announce that all
     * threads have stopped.
     * - The client should use this information to enable that all threads can
     * be expanded to access their stacktraces.
     * - If the attribute is missing or false, only the thread with the given
     * `threadId` can be expanded.
     */
    public Boolean allThreadsStopped;

    /**
     * Ids of the breakpoints that triggered the event. In most cases there is
     * only a single breakpoint but here are some examples for multiple
     * breakpoints:
     * - Different types of breakpoints map to the same location.
     * - Multiple source breakpoints get collapsed to the same instruction by
     * the compiler/runtime.
     * - Multiple function breakpoints with different function names map to the
     * same location.
     */
    public int[] hitBreakpointIds;

    @Override
    public StoppedEvent get() {
        return this;
    }
}
