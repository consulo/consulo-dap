package consulo.execution.debugger.dap.protocol.event;

import consulo.execution.debugger.dap.protocol.Breakpoint;
import consulo.execution.debugger.dap.protocol.Event;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-25
 */
@Event("breakpoint")
public class BreakpointEvent implements Supplier<BreakpointEvent> {
    public String reason;

    public Breakpoint breakpoint;

    @Override
    public BreakpointEvent get() {
        return this;
    }
}
