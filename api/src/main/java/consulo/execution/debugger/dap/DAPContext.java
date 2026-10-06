package consulo.execution.debugger.dap;

import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.value.DAPValuePresentation;

/**
 * @author VISTALL
 * @since 2025-01-04
 */
public record DAPContext(DAP dap,
                         DAPValuePresentation valuePresentation,
                         SourceCodeMapper lineMapper,
                         SourceCodeMapper columnMapper) {
}
