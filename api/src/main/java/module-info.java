/**
 * @author VISTALL
 * @since 2024-12-22
 */
module consulo.execution.debugger.dap {
    requires consulo.application.api;
    requires consulo.component.api;
    requires transitive consulo.execution.debug.api;
    requires consulo.logging.api;
    requires consulo.platform.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.util.collection;
    requires consulo.util.concurrent;
    requires consulo.util.lang;
    requires consulo.virtual.file.system.api;

    exports consulo.execution.debugger.dap;
    exports consulo.execution.debugger.dap.protocol;
    exports consulo.execution.debugger.dap.protocol.event;
}
