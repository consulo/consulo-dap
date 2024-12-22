package consulo.execution.debugger.dap.impl.internal;

import consulo.execution.debugger.dap.protocol.ImplMethod;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class DAPInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(ImplMethod.class)) {
            ((DAPImpl) proxy).registerEvent((Class) args[0], (Consumer) args[1]);
            return null;
        }

        return ((DAPImpl) proxy).send(method, args);
    }
}
