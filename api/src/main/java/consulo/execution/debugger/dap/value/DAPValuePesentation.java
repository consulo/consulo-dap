package consulo.execution.debugger.dap.value;

import consulo.execution.debug.frame.XNavigatable;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debugger.dap.protocol.Variable;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-03
 */
public interface DAPValuePesentation {
    default boolean hasChildren(@Nonnull Variable variable) {
        return variable.variablesReference > 0;
    }

    default boolean isArray(@Nonnull Variable variable) {
        return false;
    }

    default boolean canNavigateToTypeSource(@Nonnull Variable variable) {
        return false;
    }

    default void computeTypeSourcePosition(@Nonnull XNavigatable navigatable, @Nonnull Variable variable) {
        throw new UnsupportedOperationException();
    }

    default void setPresentation(@Nonnull XValueNode node, @Nonnull Variable variable) {
        boolean hasChildren = hasChildren(variable);

        Image image = ExecutionDebugIconGroup.nodePrimitive();
        if (hasChildren) {
            image = ExecutionDebugIconGroup.nodeValue();
        }
        if (isArray(variable)) {
            image = ExecutionDebugIconGroup.nodeArray();
        }

        node.setPresentation(image, variable.type, StringUtil.notNullize(variable.value), hasChildren);
    }
}
