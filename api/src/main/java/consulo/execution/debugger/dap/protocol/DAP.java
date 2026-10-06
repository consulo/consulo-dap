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
    CompletableFuture<ThreadsResult> threads(ThreadsArguments arguments);

    @Nonnull
    CompletableFuture<Object> attach(AttachRequestArguments arguments);

    @Nonnull
    CompletableFuture<Object> next(NextArguments arguments);

    @Nonnull
    CompletableFuture<Object> stepIn(StepInArguments arguments);

    @Nonnull
    CompletableFuture<Object> stepOut(StepOutArguments arguments);

    @Nonnull
    CompletableFuture<EvaluateResult> evaluate(EvaluateArguments arguments);

    @Nonnull
    CompletableFuture<DisassembleResult> disassemble(DisassembleArguments arguments);

    @Nonnull
    CompletableFuture<ReadMemoryResult> readMemory(ReadMemoryArguments arguments);

    @Nonnull
    CompletableFuture<SetBreakpointsResult> setFunctionBreakpoints(SetFunctionBreakpointsArguments arguments);

    @Nonnull
    CompletableFuture<LocationsResult> locations(LocationsArguments arguments);

    @Nonnull
    CompletableFuture<Object> disconnect(DisconnectArguments arguments);

    @Nonnull
    CompletableFuture<Object> terminate(TerminateArguments arguments);

    @Nonnull
    @ImplMethod
    <R> CompletableFuture<R> request(@Nonnull String requestName, @Nonnull Object arguments, @Nonnull Class<R> resultClass);

    @ImplMethod
    <V, T extends Supplier<V>> void registerEvent(@Nonnull Class<T> eventClass, @Nonnull Consumer<V> value);

    @ImplMethod
    void close();
}
