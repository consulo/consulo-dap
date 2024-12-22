package consulo.execution.debugger.dap.protocol.event;

import consulo.execution.debugger.dap.protocol.Capabilities;
import consulo.execution.debugger.dap.protocol.Event;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
@Event("capabilities")
public class CapabilitiesEvent implements Supplier<Capabilities> {
    public Capabilities capabilities;

    @Override
    public Capabilities get() {
        return capabilities;
    }

    @Override
    public String toString() {
        return "CapabilitiesEvent{" +
            "capabilities=" + capabilities +
            '}';
    }
}
