package consulo.execution.debugger.dap;

import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.value.DAPValuePresentation;
import consulo.virtualFileSystem.VirtualFile;

import java.util.function.Function;

/**
 * @param sourceFiles a source path of the debugger - a path of the machine the debuggee runs on - to its file
 * @author VISTALL
 * @since 2025-01-04
 */
public record DAPContext(DAP dap,
                         DAPValuePresentation valuePresentation,
                         SourceCodeMapper lineMapper,
                         SourceCodeMapper columnMapper,
                         DAPDisassembly disassembly,
                         Function<String, VirtualFile> sourceFiles) {
}
