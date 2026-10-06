package consulo.execution.debugger.dap;

import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XValueGroup;
import consulo.execution.debugger.dap.protocol.Scope;
import consulo.execution.debugger.dap.protocol.VariablesArguments;
import consulo.execution.debugger.dap.value.DAPValueFactory;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class DAPScopeGroup extends XValueGroup {
    private final DAPContext myContext;
    private final Scope myScope;

    public DAPScopeGroup(DAPContext context, Scope scope) {
        super(LocalizeValue.of(scope.name == null ? "Scope" : scope.name));
        myContext = context;
        myScope = scope;
    }

    @Override
    public void computeChildren(@Nonnull XCompositeNode node) {
        myContext.dap().variables(new VariablesArguments(myScope.variablesReference)).whenComplete((result, error) -> {
            if (node.isObsolete()) {
                return;
            }
            if (error != null) {
                node.setErrorMessage(DAPDebugProcess.errorMessage(error));
                return;
            }
            node.addChildren(DAPValueFactory.build(myContext.dap(), myContext.valuePresentation(), result), true);
        });
    }
}
