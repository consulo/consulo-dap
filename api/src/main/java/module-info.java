/**
 * @author VISTALL
 * @since 2024-12-22
 */
module consulo.execution.debugger.dap {
    requires transitive consulo.execution.debug.api;

    exports consulo.execution.debugger.dap;
    exports consulo.execution.debugger.dap.protocol;
    exports consulo.execution.debugger.dap.protocol.event;
}