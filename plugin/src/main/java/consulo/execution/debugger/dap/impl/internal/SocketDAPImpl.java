package consulo.execution.debugger.dap.impl.internal;

import consulo.execution.debugger.dap.protocol.event.TerminatedEvent;
import consulo.util.io.ByteArraySequence;
import consulo.util.io.ByteSequence;
import consulo.util.io.UnsyncByteArrayOutputStream;
import consulo.util.lang.TimeoutUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public abstract class SocketDAPImpl extends DAPImpl {
    private final String myHost;
    private final int myPort;

    private Socket mySocket;

    private OutputStream myOutputStream;

    private Runnable myReadTask;

    private boolean myClosed;

    private Thread myThread;

    public SocketDAPImpl(String host, Integer port) {
        myHost = host;
        myPort = port;
        myReadTask = () -> {
            InputStream inputStream;
            try {
                inputStream = mySocket.getInputStream();
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }

            byte[] buffer = new byte[1024];

            UnsyncByteArrayOutputStream readBuff = new UnsyncByteArrayOutputStream();
            while (!myClosed) {
                try {
                    int read = inputStream.read(buffer);
                    if (read > 0) {
                        readBuff.write(buffer, 0, read);
                    }

                    while (tryRead(readBuff)) {
                        Thread.yield();
                    }
                }
                catch (IOException e) {
                    if (!myClosed) {
                        e.printStackTrace();
                    }
                    
                    myClosed = true;
                }
            }
        };

        registerEvent(TerminatedEvent.class, body -> close());
    }

    @Override
    public void close() {
        myClosed = true;

        try {
            mySocket.close();
        }
        catch (IOException ignored) {
        }
    }

    private boolean tryRead(UnsyncByteArrayOutputStream stream) throws IOException {
        ByteArraySequence seq = stream.toByteArraySequence();

        int prefixOffset = find(seq, 0, PREFIX);

        // must be at start
        if (prefixOffset != 0) {
            return false;
        }

        int suffixSeq = find(seq, prefixOffset + PREFIX.length, SUFFIX);
        if (suffixSeq == -1) {
            return false;
        }

        // TODO make better without converting to string->int
        byte[] countBytes = seq.subSequence(prefixOffset + PREFIX.length, suffixSeq).toBytes();
        String bytesCountStr = new String(countBytes, StandardCharsets.US_ASCII);
        int bytesCount = Integer.parseInt(bytesCountStr);

        int allCount = PREFIX.length + countBytes.length + SUFFIX.length + bytesCount;
        if (allCount > seq.length()) {
            // not full data
            return false;
        }

        int dataStart = suffixSeq + SUFFIX.length;
        int dataEnd = suffixSeq + SUFFIX.length + bytesCount;

        ByteSequence dataSeq = seq.subSequence(dataStart, dataEnd);
        byte[] data = dataSeq.toBytes();
        System.out.println("RECEIVE: " + new String(seq.subSequence(0, dataEnd).toBytes()));

        ForkJoinPool.commonPool().execute(() -> processData(data));

        ByteSequence left = seq.subSequence(dataEnd, seq.getLength());
        byte[] leftBytes = left.toBytes();

        stream.reset();
        stream.write(leftBytes);

        return true;
    }

    private static int find(ByteSequence seq, int offset, byte[] searchData) {
        for (int i = offset; i < seq.length(); i++) {
            byte seqByte = seq.byteAt(i);

            if (searchData[0] == seqByte) {
                boolean lookup = lookup(seq, i, searchData);
                if (lookup) {
                    return i;
                }
            }
        }

        return -1;
    }

    private static boolean lookup(ByteSequence seq, int offset, byte[] searchData) {
        for (int i = 0; i < searchData.length; i++) {
            byte searchByte = searchData[i];

            int seqIndex = offset + i;

            // out of index
            if (seqIndex >= seq.length()) {
                return false;
            }

            if (seq.byteAt(seqIndex) != searchByte) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void write(byte[] bytes) throws IOException {
        boolean startListening = false;
        if (mySocket == null) {
            Socket socket = null;
            IOException lastException = null;
            for (int i = 0; i < 10; i++) {
                try {
                    Socket tempSocket = new Socket();
                    tempSocket.setKeepAlive(true);
                    tempSocket.setReuseAddress(true);
                    tempSocket.connect(new InetSocketAddress(myHost, myPort), myPort);

                    lastException = null;
                    socket = tempSocket;
                    break;
                }
                catch (SocketException c) {
                    lastException = c;
                    TimeoutUtil.sleep(1000L);
                }
            }

            if (lastException != null) {
                throw lastException;
            }

            mySocket = Objects.requireNonNull(socket);
            myOutputStream = socket.getOutputStream();
            startListening = true;
        }

        // TODO   System.out.println("SEND: " + new String(bytes));

        myOutputStream.write(bytes);

        if (startListening) {
            myThread = new Thread(myReadTask, "DAP");
            myThread.start();
        }
    }
}
