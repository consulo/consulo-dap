package consulo.execution.debugger.dap;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.breakpoint.XBreakpointHandler;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.execution.debug.breakpoint.XLineBreakpointType;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debugger.dap.protocol.*;
import consulo.execution.debugger.dap.protocol.event.*;
import consulo.execution.debugger.dap.value.DAPValuePresentation;
import consulo.execution.debugger.dap.value.DefaultDAPValuePresentation;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.UIAccess;
import consulo.util.collection.MultiMap;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-25
 */
public abstract class DAPDebugProcess extends XDebugProcess {
    private static final Logger LOG = Logger.getInstance(DAPDebugProcess.class);

    private volatile boolean myDapCreated;

    private final LazyValue<DAP> myDapCache = LazyValue.atomicNotNull(() -> {
        DAP dap = createDAP(Application.get().getInstance(DAPFactory.class));
        init(dap);
        myDapCreated = true;
        return dap;
    });

    private final Capabilities capabilities = new Capabilities();

    private final Map<Integer, XLineBreakpoint> myBreakpointMapping = new ConcurrentHashMap<>();

    private final Map<Integer, String> myThreads = new ConcurrentHashMap<>();

    private final XBreakpointHandler<?>[] myHandlers;

    private volatile boolean myBreakpointsInitialized;

    private final Supplier<DAPValuePresentation> myValuePresentation = LazyValue.notNull(this::createPresentation);

    private final InitializeRequestArguments myInitializeRequestArguments = new InitializeRequestArguments();

    private final CompletableFuture<Object> myDebuggeeRequested = new CompletableFuture<>();

    private volatile SourceCodeMapper myLineMapper = SourceCodeMapper.ZERO_BASED;
    private volatile SourceCodeMapper myColumnMapper = SourceCodeMapper.ZERO_BASED;

    private volatile int myLastStoppedThreadId = -1;

    private volatile @Nullable String myRunToPath;
    private volatile int myRunToLine = -1;
    private volatile @Nullable Integer myRunToBreakpointId;

    public DAPDebugProcess(@Nonnull XDebugSession session) {
        super(session);

        Class<? extends XLineBreakpointType> lineBreakpointType = getLineBreakpointType().getClass();
        myHandlers = new XBreakpointHandler[]{new DAPLineBreakpointHandler(lineBreakpointType, this)};
    }

    @Nonnull
    protected abstract XLineBreakpointType<?> getLineBreakpointType();

    @Nonnull
    protected Collection<? extends XLineBreakpoint<?>> getLineBreakpoints() {
        return XDebuggerManager.getInstance(getSession().getProject()).getBreakpointManager().getBreakpoints(getLineBreakpointType());
    }

    @Nonnull
    @Override
    public XBreakpointHandler<?>[] getBreakpointHandlers() {
        return myHandlers;
    }

    protected abstract DAP createDAP(DAPFactory factory);

    protected abstract String getAdapterId();

    @Nonnull
    protected DAP getDAP() {
        return myDapCache.get();
    }

    @Nonnull
    public Capabilities getAdapterCapabilities() {
        return capabilities;
    }

    @Nonnull
    public DAPContext createContext() {
        return new DAPContext(getDAP(), myValuePresentation.get(), myLineMapper, myColumnMapper);
    }

    public void start() {
        Application.get().executeOnPooledThread(this::initializeAsync);
    }

    protected DAPValuePresentation createPresentation() {
        return new DefaultDAPValuePresentation();
    }

    protected void init(DAP dap) {
        dap.registerEvent(CapabilitiesEvent.class, c -> {
            merge(capabilities, c);
            onUpdateCapabilities(myInitializeRequestArguments, capabilities);
        });

        dap.registerEvent(InitializedEvent.class, o -> onInitialized());

        dap.registerEvent(OutputEvent.class, this::onOutput);

        dap.registerEvent(BreakpointEvent.class, this::onBreakpoint);

        dap.registerEvent(ThreadEvent.class, threadEvent -> {
            switch (threadEvent.reason) {
                case ThreadEvent.THREAD_STARTED: {
                    myThreads.put(threadEvent.threadId, "Thread: " + threadEvent.threadId);
                    break;
                }
                case ThreadEvent.THREAD_EXITED: {
                    myThreads.remove(threadEvent.threadId);
                    break;
                }
            }
        });

        dap.registerEvent(StoppedEvent.class, this::onStopped);

        dap.registerEvent(ExitedEvent.class, this::onExited);

        dap.registerEvent(TerminatedEvent.class, event -> onTerminated());
    }

    public StackFrame[] getStackTraces(Integer threadId) {
        StackTraceArguments arguments = new StackTraceArguments();
        if (threadId != null) {
            arguments.threadId = threadId;
        }

        try {
            StackTraceResult result = getDAP().stackTrace(arguments).get();
            return result.stackFrames;
        }
        catch (InterruptedException | ExecutionException e) {
            throw new ProcessCanceledException(e);
        }
    }

    public Map<Integer, String> getThreads() {
        return myThreads;
    }

    protected void onStopped(StoppedEvent event) {
        int threadId = event.threadId == null ? -1 : event.threadId;
        myLastStoppedThreadId = threadId;

        Integer runToBreakpointId = myRunToBreakpointId;
        String runToPath = myRunToPath;
        if (runToPath != null) {
            myRunToPath = null;
            myRunToLine = -1;
            myRunToBreakpointId = null;
            updateBreakpoints(runToPath);
        }

        XLineBreakpoint breakpoint = null;
        if (event.hitBreakpointIds != null) {
            for (int hitBreakpointId : event.hitBreakpointIds) {
                if (runToBreakpointId != null && runToBreakpointId == hitBreakpointId) {
                    continue;
                }
                breakpoint = myBreakpointMapping.get(hitBreakpointId);
                if (breakpoint != null) {
                    break;
                }
            }
        }
        doPause(breakpoint, threadId);
    }

    protected void doPause(@Nullable XBreakpoint<?> breakpoint, int threadId) {
        DAP dap = getDAP();
        DAPContext context = createContext();

        dap.threads(new ThreadsArguments()).thenCompose(threadsResult -> {
            List<consulo.execution.debugger.dap.protocol.Thread> threads = threadsResult.threads == null ? List.of() : threadsResult.threads;
            int activeThreadId = threadId >= 0 || threads.isEmpty() ? threadId : threads.get(0).id;
            StackTraceArguments arguments = new StackTraceArguments();
            arguments.threadId = activeThreadId;
            return dap.stackTrace(arguments)
                .thenApply(trace -> new DAPSuspendContext(context, threads, activeThreadId, trace.stackFrames));
        }).whenCompleteAsync((suspendContext, error) -> {
            if (error != null) {
                LOG.warn("Failed to read the stopped state", error);
                getSession().positionReached(new DAPSuspendContext(context, List.of(), threadId, null));
                return;
            }

            if (breakpoint != null) {
                if (!getSession().breakpointReached(breakpoint, null, suspendContext)) {
                    resume(suspendContext);
                }
            }
            else {
                getSession().positionReached(suspendContext);
            }
        }, getExecutor());
    }

    protected void onInitialized() {
        List<CompletableFuture<?>> breakpoints = new ArrayList<>();
        ReadAction.run(() -> {
            getSession().initBreakpoints();
            breakpoints.addAll(registerBreakpointsInBatch());
            myBreakpointsInitialized = true;
        });

        CompletableFuture.allOf(breakpoints.toArray(CompletableFuture[]::new))
            .handle((o, error) -> null)
            .thenCompose(o -> myDebuggeeRequested)
            .thenAccept(o -> {
                if (!Boolean.FALSE.equals(capabilities.supportsConfigurationDoneRequest)) {
                    getDAP().configurationDone(createConfigurationDoneArguments());
                }
            });
    }

    protected void onBreakpoint(BreakpointEvent event) {
        if (event.breakpoint == null || event.breakpoint.id == null) {
            return;
        }

        XLineBreakpoint lineBreakpoint = myBreakpointMapping.get(event.breakpoint.id);
        if (lineBreakpoint == null) {
            return;
        }

        if ("changed".equals(event.reason)) {
            updateBreakpointState(lineBreakpoint, event.breakpoint);
        }
        else if ("removed".equals(event.reason)) {
            myBreakpointMapping.remove(event.breakpoint.id);
        }
    }

    protected void onExited(ExitedEvent event) {
        getSession().getConsoleView().print("\nProcess finished with exit code " + event.exitCode + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    }

    protected void onTerminated() {
        getSession().stop();
    }

    private List<CompletableFuture<?>> registerBreakpointsInBatch() {
        Collection<? extends XLineBreakpoint<?>> breakpoints = getLineBreakpoints();
        if (breakpoints.isEmpty()) {
            return List.of();
        }

        MultiMap<String, XLineBreakpoint<?>> map = MultiMap.create();
        for (XLineBreakpoint<?> breakpoint : breakpoints) {
            if (!breakpoint.isEnabled()) {
                continue;
            }
            String path = breakpoint.getPresentableFilePath();
            map.putValue(path, breakpoint);
        }

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Map.Entry<String, Collection<XLineBreakpoint<?>>> entry : map.entrySet()) {
            futures.add(registerBreakpoints(entry.getKey(), entry.getValue()));
        }
        return futures;
    }

    protected void updateBreakpoints(XLineBreakpoint<?> breakpoint, boolean remove) {
        if (!myBreakpointsInitialized) {
            return;
        }

        if (remove) {
            myBreakpointMapping.values().remove(breakpoint);
        }

        updateBreakpoints(breakpoint.getPresentableFilePath());
    }

    private CompletableFuture<?> updateBreakpoints(String path) {
        Collection<? extends XLineBreakpoint<?>> breakpoints = ReadAction.compute(this::getLineBreakpoints);
        List<? extends XLineBreakpoint<?>> byPath = breakpoints
            .stream()
            .filter(it -> it.isEnabled() && Objects.equals(it.getPresentableFilePath(), path))
            .toList();
        return registerBreakpoints(path, byPath);
    }

    protected CompletableFuture<?> registerBreakpoints(String filePath, Collection<? extends XLineBreakpoint<?>> breakpoints) {
        DAP dap = getDAP();

        List<XLineBreakpoint<?>> result = new ArrayList<>(breakpoints);

        SetBreakpointsArguments arguments = new SetBreakpointsArguments();
        arguments.source = new Source();
        arguments.source.path = filePath;

        boolean conditions = Boolean.TRUE.equals(capabilities.supportsConditionalBreakpoints);
        List<SourceBreakpoint> sourceBreakpoints = new ArrayList<>(result.size() + 1);
        for (XLineBreakpoint<?> breakpoint : result) {
            SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
            sourceBreakpoint.line = myLineMapper.toDAP(breakpoint.getLine());
            XExpression condition = breakpoint.getConditionExpression();
            if (conditions && condition != null && !StringUtil.isEmptyOrSpaces(condition.getExpression())) {
                sourceBreakpoint.condition = condition.getExpression();
            }
            sourceBreakpoints.add(sourceBreakpoint);
        }

        String runToPath = myRunToPath;
        boolean runTo = runToPath != null && runToPath.equals(filePath) && myRunToLine >= 0;
        if (runTo) {
            SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
            sourceBreakpoint.line = myLineMapper.toDAP(myRunToLine);
            sourceBreakpoints.add(sourceBreakpoint);
        }

        arguments.breakpoints = sourceBreakpoints.toArray(SourceBreakpoint[]::new);

        return dap.setBreakpoints(arguments).whenComplete((setBreakpointsResult, t) -> {
            if (t != null) {
                LOG.warn(t);
                return;
            }
            if (setBreakpointsResult == null || setBreakpointsResult.breakpoints == null) {
                return;
            }

            for (int i = 0; i < setBreakpointsResult.breakpoints.length; i++) {
                Breakpoint breakpoint = setBreakpointsResult.breakpoints[i];
                if (i >= result.size()) {
                    if (runTo) {
                        myRunToBreakpointId = breakpoint.id;
                    }
                    continue;
                }
                XLineBreakpoint<?> lineBreakpoint = result.get(i);
                if (breakpoint.id != null) {
                    myBreakpointMapping.put(breakpoint.id, lineBreakpoint);
                }
                updateBreakpointState(lineBreakpoint, breakpoint);
            }
        });
    }

    protected void updateBreakpointState(XLineBreakpoint<?> lineBreakpoint, Breakpoint breakpoint) {
        XDebugSession session = getSession();
        UIAccess uiAccess = session.getProject().getUIAccess();
        if (!breakpoint.verified && "pending".equals(breakpoint.reason)) {
            return;
        }
        uiAccess.give(() -> {
            if (breakpoint.verified) {
                session.setBreakpointVerified(lineBreakpoint);
            }
            else {
                session.setBreakpointInvalid(lineBreakpoint, breakpoint.message);
            }
        });
    }

    protected void onOutput(OutputEvent event) {
        if (event.output == null || "telemetry".equals(event.category)) {
            return;
        }

        ConsoleViewContentType contentType = switch (StringUtil.notNullize(event.category, "console")) {
            case "stdout" -> ConsoleViewContentType.NORMAL_OUTPUT;
            case "stderr" -> ConsoleViewContentType.ERROR_OUTPUT;
            case "important" -> ConsoleViewContentType.SYSTEM_OUTPUT;
            default -> ConsoleViewContentType.LOG_INFO_OUTPUT;
        };
        getSession().getConsoleView().print(event.output, contentType);
    }

    protected void onUpdateCapabilities(InitializeRequestArguments arguments, Capabilities capabilities) {
        if (arguments.linesStartAt1 == null || arguments.linesStartAt1) {
            myLineMapper = SourceCodeMapper.ONE_BASED;
        }
        else {
            myLineMapper = SourceCodeMapper.ZERO_BASED;
        }

        if (arguments.columnsStartAt1 == null || arguments.columnsStartAt1) {
            myColumnMapper = SourceCodeMapper.ONE_BASED;
        }
        else {
            myColumnMapper = SourceCodeMapper.ZERO_BASED;
        }
    }

    private static void merge(Object to, Object from) {
        if (to.getClass() != from.getClass()) {
            throw new IllegalArgumentException();
        }

        try {
            Class<?> clazz = to.getClass();

            for (Field field : clazz.getDeclaredFields()) {
                Object newValue = field.get(from);
                if (newValue != null) {
                    field.set(to, newValue);
                }
            }
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    private void initializeAsync() {
        DAP dap = getDAP();

        InitializeRequestArguments arguments = createInitializeRequestArguments();
        merge(myInitializeRequestArguments, arguments);

        String ideName = Application.get().getName().get();
        arguments.clientID = ideName;
        arguments.clientName = ideName;
        arguments.adapterID = getAdapterId();

        dap.initialize(arguments).whenComplete((res, t) -> {
            if (res == null) {
                reportStartFailure(t);
                return;
            }

            merge(capabilities, res);
            onUpdateCapabilities(arguments, capabilities);

            CompletableFuture<?> debuggee = startDebuggee(dap);
            myDebuggeeRequested.complete(Boolean.TRUE);
            debuggee.whenComplete((o, error) -> {
                if (error != null) {
                    reportStartFailure(error);
                }
            });
        });
    }

    protected CompletableFuture<?> startDebuggee(DAP dap) {
        return dap.launch(createLaunchRequestArguments());
    }

    private void reportStartFailure(@Nullable Throwable error) {
        String message = error == null ? "Debug adapter did not respond" : errorMessage(error);
        LOG.warn("Debug adapter failed to start: " + message);
        getSession().reportError(message);
        getSession().stop();
    }

    @Nonnull
    protected LaunchRequestArguments createLaunchRequestArguments() {
        LaunchRequestArguments launch = new LaunchRequestArguments();
        launch.env = Platform.current().os().environmentVariables();
        return launch;
    }

    protected InitializeRequestArguments createInitializeRequestArguments() {
        InitializeRequestArguments arguments = new InitializeRequestArguments();
        arguments.linesStartAt1 = true;
        arguments.columnsStartAt1 = true;
        return arguments;
    }

    @Nonnull
    protected ConfigurationDoneArguments createConfigurationDoneArguments() {
        return new ConfigurationDoneArguments();
    }

    @Override
    public void sessionInitialized() {
        getSession().setPauseActionSupported(true);
    }

    @Override
    public void startPausing() {
        int threadId = myLastStoppedThreadId;
        if (threadId < 0) {
            threadId = myThreads.keySet().stream().findFirst().orElse(1);
        }
        report(getDAP().pause(new PauseArguments(threadId)));
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        step(context, (dap, threadId) -> dap.next(new NextArguments(threadId)));
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        step(context, (dap, threadId) -> dap.stepIn(new StepInArguments(threadId)));
    }

    @Override
    public void startForceStepInto(@Nullable XSuspendContext context) {
        step(context, (dap, threadId) -> dap.stepIn(new StepInArguments(threadId)));
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        step(context, (dap, threadId) -> dap.stepOut(new StepOutArguments(threadId)));
    }

    @Override
    public void runToPosition(@Nonnull XSourcePosition position, @Nullable XSuspendContext context) {
        String path = position.getFile().getPath();
        myRunToPath = path;
        myRunToLine = position.getLine();
        updateBreakpoints(path).whenComplete((o, error) -> resume(context));
    }

    @Override
    public void resume(@Nullable XSuspendContext context) {
        ContinueArguments arguments = new ContinueArguments();
        arguments.threadId = activeThreadId(context);
        report(getDAP().continue_(arguments));
    }

    @Nonnull
    @Override
    public AsyncResult<Void> stopAsync() {
        AsyncResult<Void> result = AsyncResult.undefined();
        Task.Backgroundable.queue(getSession().getProject(), "Waiting for debugger response...", indicator -> {
            stopImpl();
            result.setDone();
        });
        return result;
    }

    protected void stopImpl() {
        if (!myDapCreated) {
            return;
        }

        DAP dap = getDAP();
        DisconnectArguments arguments = new DisconnectArguments();
        arguments.terminateDebuggee = true;
        try {
            dap.disconnect(arguments).get(2, TimeUnit.SECONDS);
        }
        catch (Exception ignored) {
        }
        dap.close();
    }

    @Override
    public boolean checkCanInitBreakpoints() {
        return false;
    }

    protected Executor getExecutor() {
        return Application.get().getInstance(ApplicationConcurrency.class).getExecutorService();
    }

    protected int activeThreadId(@Nullable XSuspendContext context) {
        if (context != null) {
            XExecutionStack stack = context.getActiveExecutionStack();
            if (stack instanceof DAPExecutionStack dapStack) {
                return dapStack.getThreadId();
            }
        }
        return myLastStoppedThreadId;
    }

    private void step(@Nullable XSuspendContext context, BiFunction<DAP, Integer, CompletableFuture<Object>> request) {
        report(request.apply(getDAP(), activeThreadId(context)));
    }

    private void report(CompletableFuture<?> future) {
        future.whenComplete((o, error) -> {
            if (error != null) {
                getSession().reportError(errorMessage(error));
            }
        });
    }

    public static String errorMessage(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.toString() : message;
    }
}
