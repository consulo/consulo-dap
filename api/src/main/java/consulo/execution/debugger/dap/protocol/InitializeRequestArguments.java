package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public class InitializeRequestArguments {
    /**
     * The ID of the client using this adapter.
     */
    public String clientID;

    /**
     * The human-readable name of the client using this adapter.
     */
    public String clientName;

    /**
     * The ID of the debug adapter.
     */
    public String adapterID;

    /**
     * Client supports the `startDebugging` request.
     */
    public Boolean supportsStartDebuggingRequest;

    /**
     * Client supports the `type` attribute for variables.
     */
    public Boolean supportsVariableType = true;
}
