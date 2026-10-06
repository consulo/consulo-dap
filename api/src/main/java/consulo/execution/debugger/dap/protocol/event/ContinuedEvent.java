package consulo.execution.debugger.dap.protocol.event;

import consulo.execution.debugger.dap.protocol.Event;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
@Event("continued")
public class ContinuedEvent implements Supplier<ContinuedEvent> {
    public int threadId;
    public Boolean allThreadsContinued;

    @Override
    public ContinuedEvent get() {
        return this;
    }
}
