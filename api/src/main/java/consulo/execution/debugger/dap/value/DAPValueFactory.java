package consulo.execution.debugger.dap.value;

import consulo.execution.debug.frame.XNamedValue;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.Variable;
import consulo.execution.debugger.dap.protocol.VariablesResult;

/**
 * @author VISTALL
 * @since 2025-01-03
 */
public class DAPValueFactory {
    public static XNamedValue create(DAP dap, DAPValuePresentation valuePresentation, Variable variable) {
        if (valuePresentation.hasChildren(variable)) {
            return new DAPObjectValue(dap, valuePresentation, variable);
        }
        return new DAPPrimitiveValue(valuePresentation, variable);
    }

    public static XValueChildrenList build(DAP dap, DAPValuePresentation valuePresentation, VariablesResult variablesResult) {
        XValueChildrenList children = new XValueChildrenList();

        if (variablesResult.variables == null) {
            return children;
        }
        for (Variable variable : variablesResult.variables) {
            children.add(create(dap, valuePresentation, variable));
        }

        return children;
    }
}
