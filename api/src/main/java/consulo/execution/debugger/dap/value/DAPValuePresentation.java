package consulo.execution.debugger.dap.value;

import consulo.execution.debug.frame.XNavigatable;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debugger.dap.DAPSourcePositionUtil;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.LocationsArguments;
import consulo.execution.debugger.dap.protocol.Scope;
import consulo.execution.debugger.dap.protocol.Variable;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-03
 */
public interface DAPValuePresentation {
    default boolean hasChildren(@Nonnull Variable variable) {
        return variable.variablesReference > 0;
    }

    default boolean isArray(@Nonnull Variable variable) {
        return false;
    }

    default boolean canNavigateToSource(@Nonnull Variable variable) {
        return variable.declarationLocationReference != null && variable.declarationLocationReference > 0;
    }

    default void computeSourcePosition(@Nonnull DAP dap, @Nonnull XNavigatable navigatable, @Nonnull Variable variable) {
        Integer reference = variable.declarationLocationReference;
        if (reference == null || reference <= 0) {
            navigatable.setSourcePosition(null);
            return;
        }
        dap.locations(new LocationsArguments(reference)).whenComplete((location, error) -> {
            if (error != null || location == null) {
                navigatable.setSourcePosition(null);
                return;
            }
            int column = location.column == null ? 0 : location.column - 1;
            navigatable.setSourcePosition(DAPSourcePositionUtil.createPosition(location.source, location.line - 1, column));
        });
    }

    default boolean canNavigateToTypeSource(@Nonnull Variable variable) {
        return false;
    }

    default void computeTypeSourcePosition(@Nonnull XNavigatable navigatable, @Nonnull Variable variable) {
        navigatable.setSourcePosition(null);
    }

    default boolean isInlineScope(@Nonnull Scope scope) {
        return "arguments".equals(scope.presentationHint) || "locals".equals(scope.presentationHint);
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
