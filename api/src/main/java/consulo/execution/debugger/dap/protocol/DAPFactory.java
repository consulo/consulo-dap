package consulo.execution.debugger.dap.protocol;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DAPFactory {
    @Nonnull
    DAP createSocketDAP(String host, int port);

    @Nonnull
    DAP createStreamDAP(InputStream input, OutputStream output);
}
