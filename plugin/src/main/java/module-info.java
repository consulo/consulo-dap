/**
 * @author VISTALL
 * @since 24/01/2023
 */
module consulo.dap {
    requires consulo.execution.debug.api;
    requires consulo.execution.debugger.dap;
    requires consulo.logging.api;
    requires consulo.proxy;
    requires consulo.util.io;
    requires consulo.util.lang;

    requires com.google.gson;

    opens consulo.execution.debugger.dap.impl.internal to consulo.proxy;
}
