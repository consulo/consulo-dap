package consulo.execution.debugger.dap;

import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debugger.dap.protocol.EvaluateArguments;
import consulo.execution.debugger.dap.protocol.EvaluateContext;
import consulo.execution.debugger.dap.protocol.Variable;
import consulo.execution.debugger.dap.value.DAPValueFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class DAPEvaluator extends XDebuggerEvaluator {
    private final DAPContext myContext;
    private final int myFrameId;

    public DAPEvaluator(DAPContext context, int frameId) {
        myContext = context;
        myFrameId = frameId;
    }

    @Override
    public void evaluate(@Nonnull String expression, @Nonnull XEvaluationCallback callback, @Nullable XSourcePosition expressionPosition) {
        myContext.dap().evaluate(new EvaluateArguments(expression, myFrameId, EvaluateContext.WATCH)).whenComplete((result, error) -> {
            if (error != null) {
                callback.errorOccurred(DAPDebugProcess.errorMessage(error));
                return;
            }

            Variable variable = new Variable();
            variable.name = expression;
            variable.evaluateName = expression;
            variable.value = result.result;
            variable.type = result.type;
            variable.variablesReference = result.variablesReference;
            variable.namedVariables = result.namedVariables;
            variable.indexedVariables = result.indexedVariables;
            variable.memoryReference = result.memoryReference;
            callback.evaluated(DAPValueFactory.create(myContext.dap(), myContext.valuePresentation(), variable));
        });
    }
}
