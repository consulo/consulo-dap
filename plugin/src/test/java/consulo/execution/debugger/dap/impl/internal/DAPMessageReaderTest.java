package consulo.execution.debugger.dap.impl.internal;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class DAPMessageReaderTest {
    private final List<String> myMessages = new ArrayList<>();
    private final DAPMessageReader myReader = new DAPMessageReader(body -> myMessages.add(new String(body, StandardCharsets.UTF_8)));

    @Test
    public void crlfHeaders() throws IOException {
        feed("Content-Length: 2\r\n\r\n{}");
        assertEquals(List.of("{}"), myMessages);
        assertFalse(myReader.hasPartialMessage());
    }

    @Test
    public void lfHeadersAsProcessTextEventsDeliverThem() throws IOException {
        feed("Content-Length: 2\n\n{}");
        assertEquals(List.of("{}"), myMessages);
    }

    @Test
    public void messageSplitAcrossChunks() throws IOException {
        String body = "{\"type\":\"event\",\"text\":\"привет\"}";
        int length = body.getBytes(StandardCharsets.UTF_8).length;
        byte[] bytes = ("Content-Length: " + length + "\r\n\r\n" + body).getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            myReader.feed(new byte[]{b}, 0, 1);
        }
        assertEquals(List.of(body), myMessages);
    }

    @Test
    public void severalMessagesInOneChunk() throws IOException {
        feed("Content-Length: 1\r\n\r\n1Content-Length: 1\r\nContent-Type: application/vscode-jsonrpc\r\n\r\n2Content-Length: 1\r\n");
        assertEquals(List.of("1", "2"), myMessages);
        assertTrue(myReader.hasPartialMessage());
        feed("\r\n3");
        assertEquals(List.of("1", "2", "3"), myMessages);
    }

    @Test
    public void badContentLength() {
        try {
            feed("Content-Length: x\r\n\r\n");
            fail();
        }
        catch (IOException expected) {
        }
    }

    private void feed(String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        myReader.feed(bytes, 0, bytes.length);
    }
}
