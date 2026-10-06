package consulo.execution.debugger.dap.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.DAPFactory;
import consulo.process.ProcessHandler;
import consulo.proxy.advanced.AdvancedProxyBuilder;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
@Singleton
@ServiceImpl
public class DAPFactoryImpl implements DAPFactory {
    private final ApplicationConcurrency myConcurrency;

    @Inject
    public DAPFactoryImpl(ApplicationConcurrency concurrency) {
        myConcurrency = concurrency;
    }

    @Nonnull
    @Override
    public DAP createSocketDAP(String host, int port) {
        SocketDAPImpl dap = AdvancedProxyBuilder.create(SocketDAPImpl.class)
            .withInvocationHandler(new DAPInvocationHandler())
            .withSuperConstructorArguments(host, port)
            .build();
        dap.setEventExecutor(myConcurrency.createBoundedApplicationPoolExecutor("DAP Events", myConcurrency.executor(), 1));
        dap.setReaderExecutor(myConcurrency.createBoundedApplicationPoolExecutor("DAP Reader", myConcurrency.executor(), 1));
        return dap;
    }

    @Nonnull
    @Override
    public DAP createStreamDAP(InputStream input, OutputStream output) {
        StreamDAPImpl dap = AdvancedProxyBuilder.create(StreamDAPImpl.class)
            .withInvocationHandler(new DAPInvocationHandler())
            .build();
        dap.setEventExecutor(myConcurrency.createBoundedApplicationPoolExecutor("DAP Events", myConcurrency.executor(), 1));
        dap.setReaderExecutor(myConcurrency.createBoundedApplicationPoolExecutor("DAP Reader", myConcurrency.executor(), 1));
        dap.startStreams(input, output);
        return dap;
    }

    @Nonnull
    @Override
    public DAP createProcessDAP(ProcessHandler processHandler) {
        ProcessDAPImpl dap = AdvancedProxyBuilder.create(ProcessDAPImpl.class)
            .withInvocationHandler(new DAPInvocationHandler())
            .build();
        dap.setEventExecutor(myConcurrency.createBoundedApplicationPoolExecutor("DAP Events", myConcurrency.executor(), 1));
        dap.startProcess(processHandler);
        return dap;
    }
}
