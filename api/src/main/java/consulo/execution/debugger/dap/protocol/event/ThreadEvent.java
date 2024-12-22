package consulo.execution.debugger.dap.protocol.event;

import consulo.execution.debugger.dap.protocol.Event;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
@Event("thread")
public class ThreadEvent implements Supplier<ThreadEvent> {
    public static final String THREAD_STARTED = "started";
    public static final String THREAD_EXITED = "exited";

    /**
     * The reason for the event.
     * Values: 'started', 'exited', etc.
     */
    public String reason;

    /**
     * The identifier of the thread.
     */
    public int threadId;

    @Override
    public ThreadEvent get() {
        return this;
    }

    @Override
    public String toString() {
        return "ThreadEvent{" +
            "reason='" + reason + '\'' +
            ", threadNumber=" + threadId +
            '}';
    }
}
