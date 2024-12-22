package consulo.execution.debugger.dap.protocol;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public class LaunchRequestArguments {
    /**
     * If true, the launch request should launch the program without enabling
     * debugging.
     */
    public Boolean noDebug;

    // region custom parameters from netcoredbg - custom parameters from vscode?
    public Map<String, String> env;
    public Boolean justMyCode;  // true
    public Boolean enableStepFiltering;// true
    public Boolean stopAtEntry;

    // this parameters for launching program from launch command
    public String program;
    public String[] args;
}
