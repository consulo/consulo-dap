package consulo.execution.debugger.dap.impl.internal;

import consulo.logging.Logger;
import jakarta.annotation.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public abstract class StreamDAPImpl extends DAPImpl {
    private static final Logger LOG = Logger.getInstance(StreamDAPImpl.class);

    private volatile @Nullable Executor myReaderExecutor;
    private volatile @Nullable InputStream myInput;
    private volatile @Nullable OutputStream myOutput;
    private volatile boolean myClosed;

    public void setReaderExecutor(Executor readerExecutor) {
        myReaderExecutor = readerExecutor;
    }

    public void startStreams(InputStream input, OutputStream output) {
        Executor readerExecutor = myReaderExecutor;
        if (readerExecutor == null) {
            throw new IllegalStateException("Reader executor is not set");
        }
        myInput = input;
        myOutput = output;
        readerExecutor.execute(() -> readLoop(input));
    }

    @Override
    protected void write(byte[] bytes) throws IOException {
        OutputStream output = myOutput;
        if (output == null || myClosed) {
            throw new IOException("Debug adapter is not connected");
        }
        output.write(bytes);
        output.flush();
    }

    @Override
    public void close() {
        myClosed = true;
        closeQuietly(myOutput);
        closeQuietly(myInput);
    }

    protected boolean isClosed() {
        return myClosed;
    }

    private void readLoop(InputStream input) {
        Throwable error = null;
        DAPMessageReader reader = new DAPMessageReader(this::processData);
        byte[] buffer = new byte[8192];
        try (input) {
            int read;
            while (!myClosed && (read = input.read(buffer)) >= 0) {
                reader.feed(buffer, 0, read);
            }
            if (!myClosed && reader.hasPartialMessage()) {
                throw new EOFException("Unexpected end of debug adapter stream");
            }
        }
        catch (IOException e) {
            if (!myClosed) {
                error = e;
                LOG.warn("Debug adapter connection failed", e);
            }
        }
        finally {
            myClosed = true;
            onConnectionClosed(error);
        }
    }

    private static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        }
        catch (Exception ignored) {
        }
    }
}
