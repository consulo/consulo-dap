package consulo.execution.debugger.dap;

import consulo.execution.debugger.dap.protocol.DAP;
import consulo.execution.debugger.dap.value.DAPValuePesentation;

/**
 * @author VISTALL
 * @since 2025-01-04
 */
public record DAPContext(DAP dap,
                         DAPValuePesentation valuePesentation,
                         SourceCodeMapper lineMapper,
                         SourceCodeMapper columnMapper) {
}
