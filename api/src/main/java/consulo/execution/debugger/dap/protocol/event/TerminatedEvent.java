package consulo.execution.debugger.dap.protocol.event;

import consulo.execution.debugger.dap.protocol.Event;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
@Event("terminated")
public class TerminatedEvent implements Supplier<TerminatedEvent> {
    public Object restart;

    @Override
    public TerminatedEvent get() {
        return this;
    }

    @Override
    public String toString() {
        return "TerminatedEvent{" +
            "restart=" + restart +
            '}';
    }
}
