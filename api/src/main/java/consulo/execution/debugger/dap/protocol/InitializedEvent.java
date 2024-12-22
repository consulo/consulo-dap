package consulo.execution.debugger.dap.protocol;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
@Event("initialized")
public class InitializedEvent implements Supplier<Object> {
    @Override
    public Object get() {
        return null;
    }
}
