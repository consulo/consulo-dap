package consulo.execution.debugger.dap.protocol;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class ThreadsResult implements Supplier<List<Thread>> {
    public List<Thread> threads = List.of();

    @Override
    public List<Thread> get() {
        return threads;
    }
}
