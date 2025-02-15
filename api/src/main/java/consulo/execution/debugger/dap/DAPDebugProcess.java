package consulo.execution.debugger.dap;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.breakpoint.XBreakpointHandler;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.execution.debug.breakpoint.XLineBreakpointType;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debugger.dap.protocol.*;
import consulo.execution.debugger.dap.protocol.event.*;
import consulo.execution.debugger.dap.value.DAPValuePesentation;
import consulo.execution.debugger.dap.value.DefaultDAPValuePesentation;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.UIAccess;
import consulo.util.collection.MultiMap;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-25
 */
public abstract class DAPDebugProcess extends XDebugProcess {
    private static final Logger LOG = Logger.getInstance(DAPDebugProcess.class);

    private final LazyValue<DAP> myDapCache = LazyValue.atomicNotNull(() -> {
        DAP dap = createDAP(Application.get().getInstance(DAPFactory.class));
        init(dap);
        return dap;
    });

    private final Capabilities capabilities = new Capabilities();

    private Map<Integer, XLineBreakpoint> myBreakpointMapping = new ConcurrentHashMap<>();

    private Map<Integer, String> myThreads = new ConcurrentHashMap<>();

    private final XBreakpointHandler<?>[] myHandlers;

    private boolean myBreakpointsInitialized;

    private final Supplier<DAPValuePesentation> myValuePresentation = LazyValue.notNull(this::createPresentation);

    private InitializeRequestArguments myInitializeRequestArguments = new InitializeRequestArguments();

    private SourceCodeMapper myLineMapper = SourceCodeMapper.ZERO_BASED;
    private SourceCodeMapper myColumnMapper = SourceCodeMapper.ZERO_BASED;

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

    public void start() {
        Application.get().executeOnPooledThread(this::initializeAsync);
    }

    protected DAPValuePesentation createPresentation() {
        return new DefaultDAPValuePesentation();
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

        dap.registerEvent(StoppedEvent.class, v -> {
            if (v.hitBreakpointIds != null && v.hitBreakpointIds.length > 0) {
                for (int hitBreakpointId : v.hitBreakpointIds) {
                    XLineBreakpoint breakpoint = myBreakpointMapping.get(hitBreakpointId);
                    if (breakpoint != null) {
                        doPause(breakpoint, v.threadId);
                        break;
                    }
                }
            }
            else {
                doPause(null, v.threadId);
            }
        });
    }

    public StackFrame[] getStackTraces(Integer threadId) {
        StackTraceArguments arguments = new StackTraceArguments();
        if (threadId != null) {
            arguments.threadId = threadId;
        }

        try {
            StackTraceResult result = myDapCache.get().stackTrace(arguments).get();
            return result.stackFrames;
        }
        catch (InterruptedException | ExecutionException e) {
            throw new ProcessCanceledException(e);
        }
    }

    public Map<Integer, String> getThreads() {
        return myThreads;
    }

    protected void doPause(@Nullable XBreakpoint<?> breakpoint, int threadId) {
        DAP dap = myDapCache.get();

        ThreadsResult threads;
        DAPSuspendContext suspendContext;
        DAPContext context = new DAPContext(dap, myValuePresentation.get(), myLineMapper, myColumnMapper);

        try {
            threads = dap.threads(new ThreadsArguments()).get();
            suspendContext = new DAPSuspendContext(context, threads.threads, threadId);
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.warn(e);
            return;
        }

        if (breakpoint != null) {
            getSession().breakpointReached(breakpoint, null, suspendContext);
        }
        else {
            getSession().positionReached(suspendContext);
        }
    }

    protected void onInitialized() {
        ReadAction.run(() -> {
            getSession().initBreakpoints();
            registerBreakpointsInBatch();
            myBreakpointsInitialized = true;
        });
    }

    protected void onBreakpoint(BreakpointEvent event) {
        XLineBreakpoint lineBreakpoint = myBreakpointMapping.get(event.breakpoint.id);
        if (lineBreakpoint == null) {
            return;
        }

        switch (event.reason) {
            case "changed": {
                updateBreakpointState(lineBreakpoint, event.breakpoint);
                break;
            }
            case "new":
                break;
            case "removed":
                System.out.println("test");
                break;
        }
    }

    private void registerBreakpointsInBatch() {
        Collection<? extends XLineBreakpoint<?>> breakpoints = getLineBreakpoints();
        if (breakpoints.isEmpty()) {
            return;
        }

        MultiMap<String, XLineBreakpoint<?>> map = MultiMap.create();
        for (XLineBreakpoint<?> breakpoint : breakpoints) {
            String path = breakpoint.getPresentableFilePath();

            map.putValue(path, breakpoint);
        }

        for (Map.Entry<String, Collection<XLineBreakpoint<?>>> entry : map.entrySet()) {
            registerBreakpoints(entry.getKey(), entry.getValue());
        }
    }

    protected void updateBreakpoints(XLineBreakpoint<?> breakpoint, boolean remove) {
        if (!myBreakpointsInitialized) {
            return;
        }

        if (remove) {
            myBreakpointMapping.values().remove(breakpoint);
        }

        String path = breakpoint.getPresentableFilePath();

        Collection<? extends XLineBreakpoint<?>> breakpoints = getLineBreakpoints();

        List<? extends XLineBreakpoint<?>> byPath = breakpoints
            .stream()
            .filter(it -> Objects.equals(it.getPresentableFilePath(), path))
            .toList();

        registerBreakpoints(path, byPath);
    }

    protected void registerBreakpoints(String filePath, Collection<? extends XLineBreakpoint<?>> breakpoints) {
        DAP dap = myDapCache.get();

        List<XLineBreakpoint<?>> result = new ArrayList<>(breakpoints);

        SetBreakpointsArguments arguments = new SetBreakpointsArguments();
        arguments.source = new Source();
        arguments.source.path = filePath;

        List<SourceBreakpoint> sourceBreakpoints = new ArrayList<>(result.size());

        for (XLineBreakpoint<?> breakpoint : result) {
            SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
            sourceBreakpoint.line = myLineMapper.toDAP(breakpoint.getLine());

            sourceBreakpoints.add(sourceBreakpoint);
        }

        arguments.breakpoints = sourceBreakpoints.toArray(SourceBreakpoint[]::new);

        dap.setBreakpoints(arguments).whenCompleteAsync((setBreakpointsResult, t) -> {
            if (t != null) {
                LOG.warn(t);
            }
            else if (setBreakpointsResult != null) {
                for (int i = 0; i < result.size(); i++) {
                    Breakpoint breakpoint = setBreakpointsResult.breakpoints[i];

                    XLineBreakpoint<?> lineBreakpoint = result.get(i);

                    myBreakpointMapping.put(breakpoint.id, lineBreakpoint);

                    updateBreakpointState(lineBreakpoint, breakpoint);
                }
            }
        });
    }

    protected void updateBreakpointState(XLineBreakpoint<?> lineBreakpoint, Breakpoint breakpoint) {
        XDebugSession session = getSession();
        UIAccess uiAccess = session.getProject().getUIAccess();

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
        getSession().getConsoleView().print(event.output, ConsoleViewContentType.LOG_INFO_OUTPUT);
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
        DAP dap = myDapCache.get();

        InitializeRequestArguments arguments = createInitializeRequestArguments();

        merge(myInitializeRequestArguments, arguments);

        String ideName = Application.get().getName().get();

        arguments.clientID = ideName;
        arguments.clientName = ideName;
        arguments.adapterID = getAdapterId();

        dap.initialize(arguments).whenCompleteAsync((res, t) -> {
            if (res != null) {
                merge(capabilities, res);

                onUpdateCapabilities(arguments, capabilities);

                LaunchRequestArguments launch = createLaunchRequestArguments();

                dap.launch(launch).whenCompleteAsync((lResult, t1) -> {
                    if (lResult != null) {
                        dap.configurationDone(createConfigurationDoneArguments());
                    }
                });
            }
            else if (t != null) {
                LOG.warn(t);
            }
        });
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
    public void resume(@Nullable XSuspendContext context) {
        ContinueArguments arguments = new ContinueArguments();
        if (context != null) {
            XExecutionStack stack = context.getActiveExecutionStack();

            if (stack instanceof DAPExecutionStack dapStack) {
                arguments.threadId = dapStack.getThreadId();
            }
        }

        myDapCache.get().continue_(arguments);
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
        myDapCache.get().close();
    }

    @Override
    public boolean checkCanInitBreakpoints() {
        return false;
    }
}
