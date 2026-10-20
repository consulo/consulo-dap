/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.debugger.dap;

import consulo.application.Application;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.XSourcePositionFactory;
import consulo.execution.debugger.dap.protocol.Capabilities;
import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.protocol.DisassembleArguments;
import consulo.execution.debugger.dap.protocol.DisassembleResult;
import consulo.execution.debugger.dap.protocol.DisassembledInstruction;
import consulo.execution.debugger.dap.protocol.StackFrame;
import consulo.logging.Logger;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Shows code without source as a listing in a read-only temp file, so it opens and steps like a normal file.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
public final class DAPDisassembly {
    public static final String EXTENSION = "disasm";

    private static final Logger LOG = Logger.getInstance(DAPDisassembly.class);

    private static final int INSTRUCTIONS_BEFORE = 32;
    private static final int INSTRUCTIONS_AFTER = 96;
    private static final int MAX_PREFETCH_FRAMES = 16;
    private static final Pattern UNKNOWN_NAME = Pattern.compile("\\?*");

    private record Instruction(long address, String bytes, String text, @Nullable String symbol) {
    }

    private static final class Region {
        private final Path myFile;
        private final NavigableMap<Long, Instruction> myInstructions = new TreeMap<>();
        private final Map<Long, Integer> myLines = new HashMap<>();
        private @Nullable VirtualFile myVirtualFile;

        private Region(Path file) {
            myFile = file;
        }

        private boolean contains(long address) {
            return myInstructions.containsKey(address);
        }

        private boolean overlaps(long first, long last) {
            return !myInstructions.isEmpty() && first <= myInstructions.lastKey() && last >= myInstructions.firstKey();
        }
    }

    private final Supplier<DAP> myDap;
    private final Capabilities myCapabilities;
    private final Executor myExecutor;
    private final List<Region> myRegions = new ArrayList<>();
    private @Nullable Path myDirectory;

    DAPDisassembly(Supplier<DAP> dap, Capabilities capabilities, Executor executor) {
        myDap = dap;
        myCapabilities = capabilities;
        myExecutor = executor;
    }

    public static boolean hasLocalSource(StackFrame frame) {
        String path = frame.source == null ? null : frame.source.path;
        return path != null && Files.isRegularFile(Path.of(path));
    }

    /**
     * The frame name, or {@code module!address} when the debugger has no symbol for the code (gdb reports "??").
     */
    public static String frameLabel(StackFrame frame) {
        String name = frame.name;
        if (name != null && !UNKNOWN_NAME.matcher(name.strip()).matches()) {
            return name;
        }
        String module = moduleName(frame.moduleId);
        String address = frame.instructionPointerReference;
        if (address == null) {
            return module != null ? module : (name == null ? "" : name);
        }
        return module != null ? module + "!" + address : address;
    }

    private static @Nullable String moduleName(@Nullable Object moduleId) {
        if (!(moduleId instanceof String id)) {
            return null;
        }
        // only a path gives a readable name; lldb reports a UUID
        int slash = Math.max(id.lastIndexOf('/'), id.lastIndexOf('\\'));
        return slash < 0 ? null : id.substring(slash + 1);
    }

    public synchronized @Nullable XSourcePosition findPosition(@Nullable String instructionPointerReference) {
        Long address = parseAddress(instructionPointerReference);
        if (address == null) {
            return null;
        }
        for (Region region : myRegions) {
            Integer line = region.myLines.get(address);
            if (line != null && region.myVirtualFile != null) {
                return Application.get().getInstance(XSourcePositionFactory.class).createPosition(region.myVirtualFile, line);
            }
        }
        return null;
    }

    public synchronized boolean isDisassembly(VirtualFile file) {
        for (Region region : myRegions) {
            if (file.equals(region.myVirtualFile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Disassembles the frames that have no source on disk, so {@link #findPosition} can answer without a request.
     */
    public CompletableFuture<?> prefetch(@Nullable StackFrame[] frames) {
        if (frames == null || !Boolean.TRUE.equals(myCapabilities.supportsDisassembleRequest)) {
            return CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (StackFrame frame : frames) {
            if (futures.size() == MAX_PREFETCH_FRAMES) {
                break;
            }
            if (frame.instructionPointerReference != null && !hasLocalSource(frame)) {
                futures.add(resolve(frame.instructionPointerReference, frameLabel(frame)));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    synchronized void dispose() {
        Path directory = myDirectory;
        myDirectory = null;
        myRegions.clear();
        if (directory == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                path.toFile().setWritable(true);
                Files.deleteIfExists(path);
            }
        }
        catch (IOException e) {
            LOG.warn("Failed to delete " + directory, e);
        }
        LocalFileSystem.getInstance().refreshIoFiles(List.of(directory.toFile()), true, true, null);
    }

    private CompletableFuture<?> resolve(String instructionPointerReference, @Nullable String frameName) {
        Long address = parseAddress(instructionPointerReference);
        if (address == null) {
            return CompletableFuture.completedFuture(null);
        }
        synchronized (this) {
            for (Region region : myRegions) {
                if (region.contains(address)) {
                    return CompletableFuture.completedFuture(null);
                }
            }
        }

        return disassemble(instructionPointerReference, -INSTRUCTIONS_BEFORE).thenCompose(result -> {
            if (contains(result, address)) {
                return CompletableFuture.completedFuture(result);
            }
            // gdb 18 mishandles a negative offset and returns a window after the address
            return disassemble(instructionPointerReference, 0);
        }).thenAcceptAsync(result -> {
            if (result != null && result.instructions != null) {
                store(result.instructions, frameName);
            }
        }, myExecutor).exceptionally(error -> {
            LOG.warn("Failed to disassemble " + instructionPointerReference, error);
            return null;
        });
    }

    private CompletableFuture<DisassembleResult> disassemble(String instructionPointerReference, int instructionOffset) {
        DisassembleArguments arguments = new DisassembleArguments(instructionPointerReference, INSTRUCTIONS_BEFORE + INSTRUCTIONS_AFTER);
        arguments.instructionOffset = instructionOffset;
        arguments.resolveSymbols = true;
        return myDap.get().disassemble(arguments);
    }

    private static boolean contains(@Nullable DisassembleResult result, long address) {
        if (result == null || result.instructions == null) {
            return false;
        }
        for (DisassembledInstruction instruction : result.instructions) {
            Long instructionAddress = parseAddress(instruction.address);
            if (instructionAddress != null && instructionAddress == address) {
                return true;
            }
        }
        return false;
    }

    private void store(DisassembledInstruction[] instructions, @Nullable String frameName) {
        List<Instruction> parsed = new ArrayList<>(instructions.length);
        for (DisassembledInstruction instruction : instructions) {
            Long address = parseAddress(instruction.address);
            if (address == null || "invalid".equals(instruction.presentationHint) || instruction.instruction == null) {
                continue;
            }
            parsed.add(new Instruction(address, normalizeBytes(instruction.instructionBytes), instruction.instruction.strip(), instruction.symbol));
        }
        if (parsed.isEmpty()) {
            return;
        }

        Path file;
        Region region;
        synchronized (this) {
            long first = parsed.getFirst().address();
            long last = parsed.getLast().address();
            region = null;
            for (Region candidate : myRegions) {
                if (candidate.overlaps(first, last)) {
                    region = candidate;
                    break;
                }
            }
            if (region == null) {
                Path directory = directory();
                if (directory == null) {
                    return;
                }
                String name = parsed.getFirst().symbol() != null ? parsed.getFirst().symbol() : frameName;
                region = new Region(directory.resolve(Long.toHexString(first)).resolve(fileName(name)));
                myRegions.add(region);
            }
            for (Instruction instruction : parsed) {
                region.myInstructions.put(instruction.address(), instruction);
            }
            file = region.myFile;
            try {
                Files.createDirectories(file.getParent());
                file.toFile().setWritable(true);
                Files.writeString(file, render(region), StandardCharsets.UTF_8);
                file.toFile().setWritable(false);
            }
            catch (IOException e) {
                LOG.warn("Failed to write " + file, e);
                return;
            }
        }

        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file);
        synchronized (this) {
            region.myVirtualFile = virtualFile;
        }
    }

    private @Nullable Path directory() {
        if (myDirectory == null) {
            try {
                myDirectory = Files.createTempDirectory("consulo-dap-disassembly");
            }
            catch (IOException e) {
                LOG.warn("Failed to create a directory for disassembly", e);
            }
        }
        return myDirectory;
    }

    /**
     * objdump-style listing: {@code address:  bytes  instruction}, with a {@code symbol:} line where a symbol starts.
     */
    private static String render(Region region) {
        int addressWidth = 0;
        int bytesWidth = 0;
        for (Instruction instruction : region.myInstructions.values()) {
            addressWidth = Math.max(addressWidth, Long.toHexString(instruction.address()).length());
            bytesWidth = Math.max(bytesWidth, instruction.bytes().length());
        }

        StringBuilder builder = new StringBuilder();
        region.myLines.clear();
        int line = 0;
        for (Instruction instruction : region.myInstructions.values()) {
            if (instruction.symbol() != null) {
                if (line > 0) {
                    builder.append('\n');
                    line++;
                }
                builder.append(instruction.symbol()).append(":\n");
                line++;
            }
            region.myLines.put(instruction.address(), line);

            String address = Long.toHexString(instruction.address());
            builder.append("  0x").append("0".repeat(addressWidth - address.length())).append(address).append(":  ");
            if (bytesWidth > 0) {
                builder.append(instruction.bytes()).append(" ".repeat(bytesWidth - instruction.bytes().length() + 2));
            }
            builder.append(instruction.text()).append('\n');
            line++;
        }
        return builder.toString();
    }

    private static String normalizeBytes(@Nullable String bytes) {
        if (bytes == null) {
            return "";
        }
        String hex = bytes.replace(" ", "").toLowerCase();
        StringBuilder builder = new StringBuilder(hex.length() * 3 / 2);
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(hex, i, Math.min(i + 2, hex.length()));
        }
        return builder.toString();
    }

    private static String fileName(@Nullable String name) {
        String base = name == null || name.isBlank() ? "disassembly" : name.replaceAll("[^A-Za-z0-9_.@!+\\-]", "_");
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        return base + "." + EXTENSION;
    }

    static @Nullable Long parseAddress(@Nullable String reference) {
        if (reference == null) {
            return null;
        }
        String value = reference.strip();
        if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        try {
            return Long.parseUnsignedLong(value, 16);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
