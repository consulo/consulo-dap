package consulo.execution.debugger.dap.value;

import consulo.execution.debug.frame.*;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.Variable;
import consulo.execution.debugger.dap.protocol.VariablesArguments;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-03
 */
public class DAPObjectValue extends XNamedValue {
    @Nonnull
    private final DAP myDap;
    @Nonnull
    private final DAPValuePesentation myValuePesentation;
    @Nonnull
    private final Variable myVariable;

    public DAPObjectValue(@Nonnull DAP dap, @Nonnull DAPValuePesentation valuePesentation, @Nonnull Variable variable) {
        super(variable.name);
        myDap = dap;
        myValuePesentation = valuePesentation;
        myVariable = variable;
    }

    @Override
    public void computePresentation(@Nonnull XValueNode node, @Nonnull XValuePlace place) {
        myValuePesentation.setPresentation(node, myVariable);
    }

    @Override
    public void computeChildren(@Nonnull XCompositeNode node) {
        myDap.variables(new VariablesArguments(myVariable.variablesReference)).whenCompleteAsync((variablesResult, throwable) -> {
            if (variablesResult != null) {
                node.addChildren(DAPValueFactory.build(myDap, myValuePesentation, variablesResult), true);
            }
        });
    }

    @Override
    public boolean canNavigateToTypeSource() {
        return myValuePesentation.canNavigateToTypeSource(myVariable);
    }

    @Override
    public void computeTypeSourcePosition(@Nonnull XNavigatable navigatable) {
        if (myValuePesentation.canNavigateToTypeSource(myVariable)) {
            myValuePesentation.computeTypeSourcePosition(navigatable, myVariable);
        }
    }
}
