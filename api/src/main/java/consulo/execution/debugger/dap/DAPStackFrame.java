package consulo.execution.debugger.dap;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.XSourcePositionFactory;
import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debugger.dap.protocol.*;
import consulo.execution.debugger.dap.value.DAPValueFactory;
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
    private final DAPContext myContext;
    private final StackFrame myStackTrace;
    private final Supplier<XSourcePosition> mySourcePositionValue;

    public DAPStackFrame(DAPContext context,
                         StackFrame stackTrace) {
        myContext = context;
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

                XSourcePositionFactory factory = Application.get().getInstance(XSourcePositionFactory.class);
                
                return factory.createPosition(file,
                    context.lineMapper().fromDAP(myStackTrace.line),
                    context.columnMapper().fromDAP(myStackTrace.column)
                );
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
        DAP dap = myContext.dap();

        dap.scopes(new ScopesArguments(myStackTrace.id)).whenCompleteAsync((scopesResult, t) -> {
            if (scopesResult != null) {
                for (Scope scope : scopesResult.scopes) {
                    dap.variables(new VariablesArguments(scope.variablesReference)).whenCompleteAsync((variablesResult, t2) -> {
                        if (variablesResult != null) {
                            node.addChildren(DAPValueFactory.build(dap, myContext.valuePesentation(), variablesResult), true);
                        }
                    });
                }
            }
        });
    }
}
