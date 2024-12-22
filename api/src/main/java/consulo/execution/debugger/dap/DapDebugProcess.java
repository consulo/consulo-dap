package consulo.execution.debugger.dap;

import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debugger.dap.protocol.Capabilities;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public abstract class DapDebugProcess extends XDebugProcess {
    private final Capabilities myCapabilities = new Capabilities();

    private LazyValue<DAP> myDAP = LazyValue.atomicNotNull(this::createDAP);
    
    public DapDebugProcess(@Nonnull XDebugSession session) {
        super(session);
    }

    protected abstract DAP createDAP();
}
