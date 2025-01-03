package consulo.execution.debugger.dap;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.XSourcePositionFactory;
import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debugger.dap.protocol.*;
import consulo.execution.debugger.dap.value.DAPValueFactory;
import consulo.execution.debugger.dap.value.DAPValuePesentation;
import consulo.ui.ex.ColoredTextContainer;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-01-02
 */
public class DAPStackFrame extends XStackFrame {
    private final DAP myDap;
    private final DAPValuePesentation myValuePesentation;
    private final StackFrame myStackTrace;

    private final Supplier<XSourcePosition> mySourcePositionValue;

    public DAPStackFrame(DAP dap, DAPValuePesentation valuePesentation, StackFrame stackTrace) {
        myDap = dap;
        myValuePesentation = valuePesentation;
        myStackTrace = stackTrace;
        mySourcePositionValue = LazyValue.nullable(() -> {
            Source source = myStackTrace.source;
            if (source == null) {
                return null;
            }

            String path = source.path;
            if (path == null) {
                return null;
            }

            return ReadAction.compute(() -> {
                VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
                if (file == null) {
                    return null;
                }

                return Application.get().getInstance(XSourcePositionFactory.class).createPosition(file, myStackTrace.line, myStackTrace.column);
            });
        });
    }

    @Nullable
    @Override
    public XSourcePosition getSourcePosition() {
        return mySourcePositionValue.get();
    }

    @Override
    public void customizePresentation(ColoredTextContainer component) {
        component.append(myStackTrace.name);
    }

    @Override
    public void computeChildren(@Nonnull XCompositeNode node) {
        myDap.scopes(new ScopesArguments(myStackTrace.id)).whenCompleteAsync((scopesResult, t) -> {
            if (scopesResult != null) {
                for (Scope scope : scopesResult.scopes) {
                    myDap.variables(new VariablesArguments(scope.variablesReference)).whenCompleteAsync((variablesResult, t2) -> {
                        if (variablesResult != null) {
                            node.addChildren(DAPValueFactory.build(myDap, myValuePesentation, variablesResult), true);
                        }
                    });
                }
            }
        });
    }
}
