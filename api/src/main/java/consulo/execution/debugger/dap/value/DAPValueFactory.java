package consulo.execution.debugger.dap.value;

import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.Variable;
import consulo.execution.debugger.dap.protocol.VariablesResult;

/**
 * @author VISTALL
 * @since 2025-01-03
 */
public class DAPValueFactory {
    public static XValueChildrenList build(DAP dap, DAPValuePesentation valuePesentation, VariablesResult variablesResult) {
        XValueChildrenList children = new XValueChildrenList();

        for (Variable variable : variablesResult.variables) {
            if (valuePesentation.hasChildren(variable)) {
                children.add(new DAPObjectValue(dap, valuePesentation, variable));
            }
            else {
                children.add(new DAPPrimitiveValue(valuePesentation, variable));
            }
        }

        return children;
    }
}
