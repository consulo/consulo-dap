package consulo.execution.debugger.dap.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.DAPFactory;
import consulo.proxy.advanced.AdvancedProxyBuilder;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
@Singleton
@ServiceImpl
public class DAPFactoryImpl implements DAPFactory {
    @Nonnull
    @Override
    public DAP createSocketDAP(String host, int port) {
        return AdvancedProxyBuilder.create(SocketDAPImpl.class)
            .withInvocationHandler(new DAPInvocationHandler())
            .withSuperConstructorArguments(host, port)
            .build();
    }
}
