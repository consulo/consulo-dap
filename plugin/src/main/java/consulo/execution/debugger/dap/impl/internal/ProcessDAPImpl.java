package consulo.execution.debugger.dap.impl.internal;

import consulo.logging.Logger;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public abstract class ProcessDAPImpl extends DAPImpl {
    private static final Logger LOG = Logger.getInstance(ProcessDAPImpl.class);

    private final DAPMessageReader myReader = new DAPMessageReader(this::processData);

    private volatile @Nullable ProcessHandler myProcessHandler;
    private volatile boolean myClosed;

    public void startProcess(ProcessHandler processHandler) {
        myProcessHandler = processHandler;
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(@Nonnull ProcessEvent event, @Nonnull Key outputType) {
                if (outputType != ProcessOutputTypes.STDOUT || myClosed) {
                    return;
                }
                byte[] bytes = event.getText().getBytes(StandardCharsets.UTF_8);
                try {
                    myReader.feed(bytes, 0, bytes.length);
                }
                catch (IOException e) {
                    LOG.warn("Debug adapter sent a malformed message", e);
                    myClosed = true;
                    processHandler.destroyProcess();
                }
            }

            @Override
            public void processTerminated(@Nonnull ProcessEvent event) {
                myClosed = true;
                onConnectionClosed(null);
            }
        });
        if (!processHandler.isStartNotified()) {
            processHandler.startNotify();
        }
    }

    @Override
    protected void write(byte[] bytes) throws IOException {
        ProcessHandler processHandler = myProcessHandler;
        OutputStream input = processHandler == null || myClosed ? null : processHandler.getProcessInput();
        if (input == null) {
            throw new IOException("Debug adapter is not connected");
        }
        input.write(bytes);
        input.flush();
    }

    @Override
    public void close() {
        myClosed = true;
        ProcessHandler processHandler = myProcessHandler;
        OutputStream input = processHandler == null ? null : processHandler.getProcessInput();
        if (input != null) {
            try {
                input.close();
            }
            catch (IOException ignored) {
            }
        }
    }
}
