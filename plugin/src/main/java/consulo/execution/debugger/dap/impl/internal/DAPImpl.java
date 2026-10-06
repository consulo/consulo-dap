package consulo.execution.debugger.dap.impl.internal;

import com.google.gson.Gson;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.Event;
import consulo.logging.Logger;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.io.UnsyncByteArrayOutputStream;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public abstract class DAPImpl implements DAP {
    private static final Logger LOG = Logger.getInstance(DAPImpl.class);

    protected record Request(CompletableFuture future, Type result) {
    }

    protected record EventListener(Type result, Consumer consumer) {
    }

    protected static final byte[] PREFIX = "Content-Length: ".getBytes(StandardCharsets.US_ASCII);
    protected static final byte[] SUFFIX = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private static final String TERMINATED = "terminated";

    private final AtomicInteger mySeq = new AtomicInteger(1);
    private final Object myWriteLock = new Object();
    private final AtomicBoolean myTerminatedDispatched = new AtomicBoolean();

    protected Map<Integer, Request> myRequestFutures = new ConcurrentHashMap<>();

    protected Map<String, List<EventListener>> myEventListeners = new ConcurrentHashMap<>();

    private volatile Executor myEventExecutor = Runnable::run;

    public void setEventExecutor(Executor eventExecutor) {
        myEventExecutor = eventExecutor;
    }

    public Object send(Method method, Object[] arguments) throws IOException {
        ParameterizedType paramType = (ParameterizedType) method.getGenericReturnType();
        Type returnType = paramType.getActualTypeArguments()[0];
        return send(method.getName(), arguments[0], returnType);
    }

    public CompletableFuture send(String commandName, Object arg, Type resultType) {
        int seq = mySeq.getAndIncrement();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("seq", seq);
        request.put("type", "request");
        if (commandName.charAt(commandName.length() - 1) == '_') {
            commandName = commandName.substring(0, commandName.length() - 1);
        }
        request.put("command", commandName);
        request.put("arguments", arg);

        CompletableFuture future = new CompletableFuture();
        myRequestFutures.put(seq, new Request(future, resultType));
        try {
            writeMessage(request);
        }
        catch (IOException e) {
            myRequestFutures.remove(seq);
            future.completeExceptionally(e);
        }
        return future;
    }

    @SuppressWarnings("unchecked")
    protected void processData(byte[] data) {
        try {
            Gson gson = new Gson();
            Map map = gson.fromJson(new InputStreamReader(new UnsyncByteArrayInputStream(data), StandardCharsets.UTF_8), Map.class);
            Object type = map.get("type");
            if ("event".equals(type)) {
                String eventName = (String) map.get("event");
                Object body = map.get("body");
                dispatchEvent(eventName, gson.toJson(body == null ? Map.of() : body));
            }
            else if ("response".equals(type)) {
                int requestSeq = ((Number) map.get("request_seq")).intValue();
                boolean success = Boolean.TRUE.equals(map.get("success"));
                Request request = myRequestFutures.remove(requestSeq);
                if (request == null) {
                    return;
                }
                Object body = map.get("body");
                if (success) {
                    Object resultObj = gson.fromJson(gson.toJson(body == null ? Map.of() : body), request.result());
                    request.future().complete(resultObj);
                }
                else {
                    request.future().completeExceptionally(new DAPRequestException(errorMessage(map, body)));
                }
            }
            else if ("request".equals(type)) {
                int requestSeq = ((Number) map.get("seq")).intValue();
                String command = String.valueOf(map.get("command"));
                respond(requestSeq, command, false, "Request '" + command + "' is not supported");
            }
        }
        catch (Exception e) {
            LOG.error("Failed to process a debug adapter message: " + new String(data, StandardCharsets.UTF_8), e);
        }
    }

    protected void onConnectionClosed(@Nullable Throwable error) {
        IOException closed = new IOException("Debug adapter connection closed", error);
        for (Request request : List.copyOf(myRequestFutures.values())) {
            request.future().completeExceptionally(closed);
        }
        myRequestFutures.clear();
        dispatchEvent(TERMINATED, "{}");
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <R> CompletableFuture<R> request(@Nonnull String requestName, @Nonnull Object arguments, @Nonnull Class<R> resultClass) {
        return send(requestName, arguments, resultClass);
    }

    @Override
    public <V, T extends Supplier<V>> void registerEvent(@Nonnull Class<T> eventClass, @Nonnull Consumer<V> value) {
        Event annotation = eventClass.getAnnotation(Event.class);
        if (annotation == null) {
            throw new IllegalArgumentException();
        }

        myEventListeners.computeIfAbsent(annotation.value(), s -> new CopyOnWriteArrayList<>()).add(new EventListener(eventClass, value));
    }

    protected abstract void write(byte[] bytes) throws IOException;

    @SuppressWarnings("unchecked")
    private void dispatchEvent(String eventName, String bodyJson) {
        if (TERMINATED.equals(eventName) && !myTerminatedDispatched.compareAndSet(false, true)) {
            return;
        }
        List<EventListener> listeners = myEventListeners.get(eventName);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        myEventExecutor.execute(() -> {
            Gson gson = new Gson();
            for (EventListener listener : listeners) {
                try {
                    Supplier resultObj = gson.fromJson(bodyJson, listener.result());
                    listener.consumer().accept(resultObj.get());
                }
                catch (Throwable e) {
                    LOG.error("Debug adapter event listener failed for '" + eventName + "'", e);
                }
            }
        });
    }

    private void respond(int requestSeq, String command, boolean success, String message) throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("seq", mySeq.getAndIncrement());
        response.put("type", "response");
        response.put("request_seq", requestSeq);
        response.put("success", success);
        response.put("command", command);
        response.put("message", message);
        writeMessage(response);
    }

    private void writeMessage(Map<String, Object> message) throws IOException {
        byte[] messageBytes = new Gson().toJson(message).getBytes(StandardCharsets.UTF_8);
        UnsyncByteArrayOutputStream buff = new UnsyncByteArrayOutputStream(messageBytes.length + 32);
        buff.write(PREFIX);
        buff.write(String.valueOf(messageBytes.length).getBytes(StandardCharsets.US_ASCII));
        buff.write(SUFFIX);
        buff.write(messageBytes);
        synchronized (myWriteLock) {
            write(buff.toByteArray());
        }
    }

    private static String errorMessage(Map response, @Nullable Object body) {
        if (body instanceof Map bodyMap && bodyMap.get("error") instanceof Map error && error.get("format") instanceof String format) {
            return format;
        }
        Object message = response.get("message");
        return message == null ? "Request failed" : String.valueOf(message);
    }
}
