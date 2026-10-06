package consulo.execution.debugger.dap;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.XSourcePositionFactory;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debugger.dap.protocol.*;
import consulo.execution.debugger.dap.value.DAPValueFactory;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    public int getFrameId() {
        return myStackTrace.id;
    }

    @Nonnull
    public StackFrame getStackFrame() {
        return myStackTrace;
    }

    @Nullable
    @Override
    public XDebuggerEvaluator getEvaluator() {
        return new DAPEvaluator(myContext, myStackTrace.id);
    }

    @Nullable
    @Override
    public XSourcePosition getSourcePosition() {
        return mySourcePositionValue.get();
    }

    @Override
    public void customizePresentation(ColoredTextContainer component) {
        component.append(StringUtil.notNullize(myStackTrace.name), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        Source source = myStackTrace.source;
        if (source != null && myStackTrace.line > 0) {
            String name = source.name != null ? source.name : source.path;
            if (name != null) {
                int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                component.append(" " + name.substring(slash + 1) + ":" + myStackTrace.line, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }
    }

    @Override
    public void computeChildren(@Nonnull XCompositeNode node) {
        DAP dap = myContext.dap();

        dap.scopes(new ScopesArguments(myStackTrace.id)).whenComplete((scopesResult, t) -> {
            if (scopesResult == null || scopesResult.scopes == null || scopesResult.scopes.length == 0) {
                if (t != null) {
                    node.setErrorMessage(DAPDebugProcess.errorMessage(t));
                }
                else {
                    node.addChildren(XValueChildrenList.EMPTY, true);
                }
                return;
            }

            List<Scope> inline = new ArrayList<>();
            List<Scope> groups = new ArrayList<>();
            for (Scope scope : scopesResult.scopes) {
                if (myContext.valuePresentation().isInlineScope(scope)) {
                    inline.add(scope);
                }
                else {
                    groups.add(scope);
                }
            }
            if (inline.isEmpty()) {
                inline.add(groups.remove(0));
            }

            List<CompletableFuture<VariablesResult>> variables = new ArrayList<>(inline.size());
            for (Scope scope : inline) {
                variables.add(dap.variables(new VariablesArguments(scope.variablesReference)));
            }
            CompletableFuture.allOf(variables.toArray(CompletableFuture[]::new)).whenComplete((o, error) -> {
                if (node.isObsolete()) {
                    return;
                }
                XValueChildrenList children = new XValueChildrenList();
                for (CompletableFuture<VariablesResult> future : variables) {
                    if (future.isCompletedExceptionally()) {
                        continue;
                    }
                    VariablesResult result = future.getNow(null);
                    if (result != null) {
                        XValueChildrenList scopeChildren = DAPValueFactory.build(dap, myContext.valuePresentation(), result);
                        for (int i = 0; i < scopeChildren.size(); i++) {
                            children.add(scopeChildren.getName(i), scopeChildren.getValue(i));
                        }
                    }
                }
                for (Scope scope : groups) {
                    children.addBottomGroup(new DAPScopeGroup(myContext, scope));
                }
                node.addChildren(children, true);
            });
        });
    }
}
