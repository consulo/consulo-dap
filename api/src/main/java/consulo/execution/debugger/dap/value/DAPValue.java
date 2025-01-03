package consulo.execution.debugger.dap.value;

import consulo.execution.debug.frame.XNamedValue;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.frame.XValuePlace;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debugger.dap.protocol.Variable;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPValue extends XNamedValue {
    @Nonnull
    private final Variable myVariable;

    public DAPValue(@Nonnull Variable variable) {
        super(variable.name);
        myVariable = variable;
    }

    @Override
    public void computePresentation(@Nonnull XValueNode node, @Nonnull XValuePlace place) {
        node.setPresentation(ExecutionDebugIconGroup.nodeValue(), null, myVariable.value, false);
    }
}
