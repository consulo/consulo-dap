package consulo.execution.debugger.dap.impl.internal;

import consulo.execution.debugger.dap.protocol.*;
import consulo.execution.debugger.dap.protocol.event.BreakpointEvent;
import consulo.execution.debugger.dap.protocol.event.ExitedEvent;
import consulo.execution.debugger.dap.protocol.event.OutputEvent;
import consulo.execution.debugger.dap.protocol.event.StoppedEvent;
import consulo.execution.debugger.dap.protocol.event.TerminatedEvent;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class StreamDAPImplTest {
    private static final long TIMEOUT = 30;

    private static Path ourSource;
    private static Path ourExecutable;
    private static int ourSquareLine;

    private final BlockingQueue<Object> myEvents = new LinkedBlockingQueue<>();
    private ExecutorService myEventExecutor;
    private Process myAdapter;
    private StreamDAPImpl myDap;

    @BeforeClass
    public static void compile() throws Exception {
        Assume.assumeTrue("gcc is not installed", runs("gcc", "--version"));
        Assume.assumeTrue("gdb with DAP is not installed", runs("gdb", "-nx", "-batch", "-i=dap", "-ex", "quit"));

        Path directory = Files.createTempDirectory("dap-stream");
        ourSource = directory.resolve("sample.c");
        try (InputStream stream = StreamDAPImplTest.class.getResourceAsStream("/dap-sample.c")) {
            assertNotNull(stream);
            Files.copy(stream, ourSource);
        }
        ourExecutable = directory.resolve("sample");
        Process gcc = new ProcessBuilder("gcc", "-g", "-O0", "-pthread", "-o", ourExecutable.toString(), ourSource.toString()).inheritIO().start();
        assertEquals(0, gcc.waitFor());

        List<String> lines = Files.readAllLines(ourSource);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("/* SQUARE */")) {
                ourSquareLine = i + 1;
            }
        }
    }

    @After
    public void tearDown() {
        if (myDap != null) {
            myDap.close();
        }
        if (myAdapter != null) {
            myAdapter.destroyForcibly();
        }
        if (myEventExecutor != null) {
            myEventExecutor.shutdownNow();
        }
    }

    @Test
    public void gdbDapSession() throws Exception {
        start();

        InitializeRequestArguments initialize = new InitializeRequestArguments();
        initialize.adapterID = "gdb";
        initialize.linesStartAt1 = true;
        initialize.columnsStartAt1 = true;
        Capabilities capabilities = get(myDap.initialize(initialize));
        assertEquals(Boolean.TRUE, capabilities.supportsConfigurationDoneRequest);
        assertEquals(Boolean.TRUE, capabilities.supportsDisassembleRequest);

        LaunchRequestArguments launch = new LaunchRequestArguments();
        launch.program = ourExecutable.toString();
        launch.cwd = ourExecutable.getParent().toString();
        CompletableFuture<Object> launched = myDap.launch(launch);
        awaitEvent(InitializedEvent.class);

        SetBreakpointsArguments breakpoints = new SetBreakpointsArguments();
        breakpoints.source = new Source();
        breakpoints.source.path = ourSource.toString();
        SourceBreakpoint breakpoint = new SourceBreakpoint();
        breakpoint.line = ourSquareLine;
        breakpoint.condition = "value == 2";
        breakpoints.breakpoints = new SourceBreakpoint[]{breakpoint};
        SetBreakpointsResult set = get(myDap.setBreakpoints(breakpoints));
        assertEquals(1, set.breakpoints.length);
        Breakpoint inserted = set.breakpoints[0];

        get(myDap.configurationDone(new ConfigurationDoneArguments()));
        get(launched);

        if (!inserted.verified) {
            BreakpointEvent changed = awaitEvent(BreakpointEvent.class);
            assertEquals("changed", changed.reason);
            assertEquals(inserted.id, changed.breakpoint.id);
            assertTrue(changed.breakpoint.verified);
        }

        StoppedEvent stopped = awaitEvent(StoppedEvent.class);
        assertEquals("breakpoint", stopped.reason);
        assertNotNull(stopped.threadId);

        StackTraceArguments stackArguments = new StackTraceArguments();
        stackArguments.threadId = stopped.threadId;
        StackTraceResult stack = get(myDap.stackTrace(stackArguments));
        StackFrame top = stack.stackFrames[0];
        assertEquals(ourSquareLine, top.line);
        assertTrue(top.name, top.name.contains("square"));
        assertNotNull(top.instructionPointerReference);

        ScopesResult scopes = get(myDap.scopes(new ScopesArguments(top.id)));
        VariablesResult variables = get(myDap.variables(new VariablesArguments(scopes.scopes[0].variablesReference)));
        assertTrue(List.of(variables.variables).stream().anyMatch(variable -> "value".equals(variable.name) && "2".equals(variable.value)));

        EvaluateResult evaluated = get(myDap.evaluate(new EvaluateArguments("value * 10", top.id, EvaluateContext.WATCH)));
        assertEquals("20", evaluated.result);

        try {
            get(myDap.evaluate(new EvaluateArguments("no_such_variable", top.id, EvaluateContext.WATCH)));
            fail();
        }
        catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no_such_variable"));
        }

        DisassembleResult disassembly = get(myDap.disassemble(new DisassembleArguments(top.instructionPointerReference, 4)));
        assertEquals(4, disassembly.instructions.length);
        assertTrue(disassembly.instructions[0].instruction.length() > 0);

        ReadMemoryResult memory = get(myDap.readMemory(new ReadMemoryArguments(top.instructionPointerReference, 4)));
        assertEquals(4, Base64.getDecoder().decode(memory.data).length);

        get(myDap.next(new NextArguments(stopped.threadId)));
        StoppedEvent stepped = awaitEvent(StoppedEvent.class);
        assertEquals("step", stepped.reason);

        ContinueArguments continueArguments = new ContinueArguments();
        continueArguments.threadId = stopped.threadId;
        get(myDap.continue_(continueArguments));
        ExitedEvent exited = awaitEvent(ExitedEvent.class);
        assertEquals(14, exited.exitCode);
        awaitEvent(TerminatedEvent.class);
    }

    @Test
    public void closedStreamFailsPendingRequestsAndTerminates() throws Exception {
        start();
        myAdapter.destroyForcibly().waitFor(TIMEOUT, TimeUnit.SECONDS);
        CompletableFuture<Capabilities> initialize = myDap.initialize(new InitializeRequestArguments());
        try {
            get(initialize);
            fail();
        }
        catch (Exception expected) {
        }
        awaitEvent(TerminatedEvent.class);
    }

    private void start() throws IOException {
        myAdapter = new ProcessBuilder("gdb", "-q", "-nx", "-i=dap").redirectError(ProcessBuilder.Redirect.DISCARD).start();
        myEventExecutor = Executors.newSingleThreadExecutor();
        myDap = createDap();
        myDap.setEventExecutor(myEventExecutor);
        myDap.registerEvent(InitializedEvent.class, event -> myEvents.add(new InitializedEvent()));
        myDap.registerEvent(StoppedEvent.class, myEvents::add);
        myDap.registerEvent(BreakpointEvent.class, myEvents::add);
        myDap.registerEvent(ExitedEvent.class, myEvents::add);
        myDap.registerEvent(TerminatedEvent.class, myEvents::add);
        myDap.registerEvent(OutputEvent.class, event -> {
        });
        myDap.startStreams(myAdapter.getInputStream(), myAdapter.getOutputStream());
    }

    private static StreamDAPImpl createDap() {
        try {
            return new ByteBuddy()
                .subclass(StreamDAPImpl.class)
                .method(ElementMatchers.isAbstract())
                .intercept(InvocationHandlerAdapter.of(new DAPInvocationHandler()))
                .make()
                .load(StreamDAPImplTest.class.getClassLoader())
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
        }
        catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T awaitEvent(Class<T> eventClass) throws InterruptedException {
        while (true) {
            Object event = myEvents.poll(TIMEOUT, TimeUnit.SECONDS);
            if (event == null) {
                throw new AssertionError("No " + eventClass.getSimpleName());
            }
            if (eventClass.isInstance(event)) {
                return (T) event;
            }
        }
    }

    private static <T> T get(CompletableFuture<T> future) throws Exception {
        try {
            return future.get(TIMEOUT, TimeUnit.SECONDS);
        }
        catch (java.util.concurrent.ExecutionException e) {
            throw e.getCause() instanceof Exception cause ? cause : e;
        }
    }

    private static boolean runs(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            return process.waitFor() == 0;
        }
        catch (IOException e) {
            return false;
        }
        catch (InterruptedException e) {
            java.lang.Thread.currentThread().interrupt();
            return false;
        }
    }
}
