package consulo.execution.debugger.dap.impl.internal;

import com.google.gson.Gson;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.Event;
import consulo.logging.Logger;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.util.io.UnsyncByteArrayOutputStream;
import jakarta.annotation.Nonnull;

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

    private AtomicInteger mySeq = new AtomicInteger(1);

    protected Map<Integer, Request> myRequestFutures = new ConcurrentHashMap<>();

    protected Map<String, List<EventListener>> myEventListeners = new ConcurrentHashMap<>();

    public Object send(Method method, Object[] arguments) throws IOException {
        ParameterizedType paramType = (ParameterizedType) method.getGenericReturnType();
        Type returnType = paramType.getActualTypeArguments()[0];
        return send(method.getName(), arguments[0], returnType);
    }

    public CompletableFuture send(String commandName, Object arg, Type resultType) {
        Map<String, Object> request = new LinkedHashMap<>();
        int seq = mySeq.getAndIncrement();

        request.put("seq", seq);
        request.put("type", "request");

        if (commandName.charAt(commandName.length() - 1) == '_') {
            commandName = commandName.substring(0, commandName.length() - 1);
        }

        request.put("command", commandName);
        request.put("arguments", arg);

        Gson gson = new Gson();

        String json = gson.toJson(request);

        byte[] requestBytes = json.getBytes(StandardCharsets.UTF_8);

        CompletableFuture future = new CompletableFuture();

        myRequestFutures.put(seq, new Request(future, resultType));

        try {
            UnsyncByteArrayOutputStream buff = new UnsyncByteArrayOutputStream(requestBytes.length + 20);
            buff.write(PREFIX);
            buff.write(String.valueOf(requestBytes.length).getBytes(StandardCharsets.US_ASCII));
            buff.write(SUFFIX);
            buff.write(requestBytes);

            write(buff.toByteArray());
        }
        catch (IOException e) {
            LOG.warn(e);

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

                List<EventListener> listeners = myEventListeners.get(eventName);
                if (listeners == null) {
                    System.out.println("IGNORED EVENT " + map);
                    return;
                }

                Object body = map.get("body");
                if (body == null) {
                    body = Map.of();
                }
                
                String bodyJson = gson.toJson(body);
                for (EventListener listener : listeners) {
                    Supplier resultObj = gson.fromJson(bodyJson, listener.result());
                    listener.consumer().accept(resultObj.get());
                }
            }
            else if ("response".equals(type)) {
                int requestSeq = ((Number) map.get("request_seq")).intValue();
                boolean success = (boolean) map.get("success");

                Request request = myRequestFutures.remove(requestSeq);
                if (request != null) {
                    if (success) {
                        Object body = map.get("body");
                        if (body == null) {
                            body = Map.of();
                        }

                        String bodyJson = gson.toJson(body);

                        Object resultObj = gson.fromJson(bodyJson, request.result());

                        request.future().complete(resultObj);
                    }
                    else {
                        request.future().completeExceptionally(new Exception(String.valueOf(map.get("message"))));
                    }
                }
                else {
                    System.out.println("UNKNOWN REQUEST " + map);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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

        myEventListeners.computeIfAbsent(annotation.value(), s -> new ArrayList<>()).add(new EventListener(eventClass, value));
    }

    protected abstract void write(byte[] bytes) throws IOException;
}
