package consulo.execution.debugger.dap.impl.internal;

import consulo.execution.debugger.dap.protocol.event.TerminatedEvent;
import consulo.util.lang.TimeoutUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public abstract class SocketDAPImpl extends StreamDAPImpl {
    private static final int CONNECT_ATTEMPTS = 10;
    private static final int CONNECT_TIMEOUT_MS = 1000;

    private final String myHost;
    private final int myPort;

    private volatile Socket mySocket;

    public SocketDAPImpl(String host, Integer port) {
        myHost = host;
        myPort = port;

        registerEvent(TerminatedEvent.class, body -> close());
    }

    @Override
    protected void write(byte[] bytes) throws IOException {
        if (mySocket == null) {
            connect();
        }
        super.write(bytes);
    }

    @Override
    public void close() {
        super.close();
        Socket socket = mySocket;
        if (socket != null) {
            try {
                socket.close();
            }
            catch (IOException ignored) {
            }
        }
    }

    private synchronized void connect() throws IOException {
        if (mySocket != null) {
            return;
        }

        IOException lastException = null;
        for (int i = 0; i < CONNECT_ATTEMPTS && !isClosed(); i++) {
            Socket socket = new Socket();
            try {
                socket.setKeepAlive(true);
                socket.setReuseAddress(true);
                socket.connect(new InetSocketAddress(myHost, myPort), CONNECT_TIMEOUT_MS);
                mySocket = socket;
                startStreams(socket.getInputStream(), socket.getOutputStream());
                return;
            }
            catch (IOException e) {
                lastException = e;
                socket.close();
                TimeoutUtil.sleep(1000L);
            }
        }
        throw lastException != null ? lastException : new IOException("Debug adapter connection is closed");
    }
}
