package consulo.execution.debugger.dap.protocol.event;

import consulo.execution.debugger.dap.protocol.Event;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
@Event("exited")
public class ExitedEvent implements Supplier<ExitedEvent> {
    public int exitCode;

    @Override
    public ExitedEvent get() {
        return this;
    }
}
