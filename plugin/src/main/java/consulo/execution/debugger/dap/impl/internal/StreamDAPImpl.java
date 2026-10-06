package consulo.execution.debugger.dap.impl.internal;

import consulo.logging.Logger;
import jakarta.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public abstract class StreamDAPImpl extends DAPImpl {
    private static final Logger LOG = Logger.getInstance(StreamDAPImpl.class);

    private volatile @Nullable InputStream myInput;
    private volatile @Nullable OutputStream myOutput;
    private volatile boolean myClosed;

    public void startStreams(InputStream input, OutputStream output) {
        myInput = input;
        myOutput = output;
        Thread reader = new Thread(() -> readLoop(input), "DAP reader");
        reader.setDaemon(true);
        reader.start();
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

    private void readLoop(InputStream stream) {
        Throwable error = null;
        try (InputStream input = new BufferedInputStream(stream)) {
            while (!myClosed) {
                int length = readContentLength(input);
                if (length < 0) {
                    break;
                }
                byte[] data = input.readNBytes(length);
                if (data.length < length) {
                    break;
                }
                processData(data);
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

    private static int readContentLength(InputStream input) throws IOException {
        int length = -1;
        while (true) {
            String line = readHeaderLine(input);
            if (line == null) {
                return -1;
            }
            if (line.isEmpty()) {
                if (length >= 0) {
                    return length;
                }
                continue;
            }
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Length")) {
                try {
                    length = Integer.parseInt(line.substring(colon + 1).trim());
                }
                catch (NumberFormatException e) {
                    throw new IOException("Bad Content-Length header: " + line, e);
                }
            }
        }
    }

    private static @Nullable String readHeaderLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (true) {
            int b = input.read();
            if (b == -1) {
                if (line.size() == 0) {
                    return null;
                }
                throw new EOFException("Unexpected end of debug adapter stream");
            }
            if (b == '\n') {
                byte[] bytes = line.toByteArray();
                int end = bytes.length > 0 && bytes[bytes.length - 1] == '\r' ? bytes.length - 1 : bytes.length;
                return new String(bytes, 0, end, StandardCharsets.US_ASCII);
            }
            line.write(b);
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
