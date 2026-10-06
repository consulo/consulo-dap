package consulo.execution.debugger.dap.value;

import consulo.execution.debug.frame.XNamedValue;
import consulo.execution.debug.frame.XNavigatable;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.frame.XValuePlace;
import consulo.execution.debugger.dap.protocol.Variable;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPPrimitiveValue extends XNamedValue {
    @Nonnull
    private final DAPValuePresentation myValuePresentation;
    @Nonnull
    private final Variable myVariable;

    public DAPPrimitiveValue(@Nonnull DAPValuePresentation valuePresentation, @Nonnull Variable variable) {
        super(variable.name);
        myValuePresentation = valuePresentation;
        myVariable = variable;
    }

    @Override
    public void computePresentation(@Nonnull XValueNode node, @Nonnull XValuePlace place) {
        myValuePresentation.setPresentation(node, myVariable);
    }

    @Override
    public boolean canNavigateToTypeSource() {
        return myValuePresentation.canNavigateToTypeSource(myVariable);
    }

    @Override
    public void computeTypeSourcePosition(@Nonnull XNavigatable navigatable) {
        if (myValuePresentation.canNavigateToTypeSource(myVariable)) {
            myValuePresentation.computeTypeSourcePosition(navigatable, myVariable);
        }
    }
}
