package consulo.execution.debugger.dap.protocol;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @see DAPFactory
 * @since 2024-12-21
 */
public interface DAP {
    @Nonnull
    CompletableFuture<Capabilities> initialize(InitializeRequestArguments arguments);

    @Nonnull
    CompletableFuture<Object> startDebugging(StartDebuggingRequestArguments arguments);

    @Nonnull
    CompletableFuture<Object> launch(LaunchRequestArguments arguments);

    @Nonnull
    CompletableFuture<ContinueResult> continue_(ContinueArguments arguments);

    @Nonnull
    CompletableFuture<Object> configurationDone(ConfigurationDoneArguments arguments);

    @Nonnull
    CompletableFuture<SetBreakpointsResult> setBreakpoints(SetBreakpointsArguments arguments);

    @Nonnull
    CompletableFuture<StackTraceResult> stackTrace(StackTraceArguments arguments);

    @Nonnull
    CompletableFuture<Object> pause(PauseArguments arguments);

    @Nonnull
    CompletableFuture<ScopesResult> scopes(ScopesArguments arguments);

    @Nonnull
    CompletableFuture<VariablesResult> variables(VariablesArguments arguments);

    @Nonnull
    @ImplMethod
    <R> CompletableFuture<R> request(@Nonnull String requestName, @Nonnull Object arguments, @Nonnull Class<R> resultClass);

    @ImplMethod
    <V, T extends Supplier<V>> void registerEvent(@Nonnull Class<T> eventClass, @Nonnull Consumer<V> value);

    @ImplMethod
    void close();
}
