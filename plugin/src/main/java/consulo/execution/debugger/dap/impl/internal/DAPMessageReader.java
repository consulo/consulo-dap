package consulo.execution.debugger.dap.impl.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public final class DAPMessageReader {
    private static final String CONTENT_LENGTH = "Content-Length";

    private final Consumer<byte[]> myConsumer;

    private byte[] myBuffer = new byte[8192];
    private int myStart;
    private int myEnd;
    private int myPendingLength = -1;
    private int myContentLength = -1;

    public DAPMessageReader(Consumer<byte[]> consumer) {
        myConsumer = consumer;
    }

    public void feed(byte[] data, int offset, int length) throws IOException {
        ensureCapacity(length);
        System.arraycopy(data, offset, myBuffer, myEnd, length);
        myEnd += length;

        while (true) {
            if (myContentLength < 0) {
                int newline = indexOf((byte) '\n');
                if (newline < 0) {
                    return;
                }
                int lineEnd = newline > myStart && myBuffer[newline - 1] == '\r' ? newline - 1 : newline;
                String line = new String(myBuffer, myStart, lineEnd - myStart, StandardCharsets.US_ASCII);
                myStart = newline + 1;
                if (line.isEmpty()) {
                    if (myPendingLength >= 0) {
                        myContentLength = myPendingLength;
                        myPendingLength = -1;
                    }
                    continue;
                }
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase(CONTENT_LENGTH)) {
                    try {
                        myPendingLength = Integer.parseInt(line.substring(colon + 1).trim());
                    }
                    catch (NumberFormatException e) {
                        throw new IOException("Bad Content-Length header: " + line, e);
                    }
                }
            }
            else {
                if (myEnd - myStart < myContentLength) {
                    return;
                }
                byte[] body = Arrays.copyOfRange(myBuffer, myStart, myStart + myContentLength);
                myStart += myContentLength;
                myContentLength = -1;
                myConsumer.accept(body);
            }
        }
    }

    public boolean hasPartialMessage() {
        return myEnd > myStart || myContentLength >= 0 || myPendingLength >= 0;
    }

    private void ensureCapacity(int length) {
        if (myStart > 0) {
            System.arraycopy(myBuffer, myStart, myBuffer, 0, myEnd - myStart);
            myEnd -= myStart;
            myStart = 0;
        }
        if (myEnd + length > myBuffer.length) {
            myBuffer = Arrays.copyOf(myBuffer, Math.max(myBuffer.length * 2, myEnd + length));
        }
    }

    private int indexOf(byte value) {
        for (int i = myStart; i < myEnd; i++) {
            if (myBuffer[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
