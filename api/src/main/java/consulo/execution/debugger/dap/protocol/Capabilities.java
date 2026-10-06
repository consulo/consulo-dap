package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public class Capabilities {
    public Boolean supportsConfigurationDoneRequest;
    public Boolean supportsFunctionBreakpoints;
    public Boolean supportsConditionalBreakpoints;
    public Boolean supportsHitConditionalBreakpoints;
    public Boolean supportsEvaluateForHovers;
    public Boolean supportsTerminateRequest;
    public Boolean supportsLogPoints;
    public Boolean supportsSteppingGranularity;
    public Boolean supportsInstructionBreakpoints;
    public Boolean supportsDisassembleRequest;
    public Boolean supportsReadMemoryRequest;
    public Boolean supportsWriteMemoryRequest;
    public Boolean supportsDataBreakpoints;
    public Boolean supportsSetVariable;
    public Boolean supportsRestartRequest;
    public Boolean supportsExceptionInfoRequest;
    public Boolean supportsModulesRequest;
    public Boolean supportsCompletionsRequest;
    public Boolean supportsSingleThreadExecutionRequests;
    public Boolean supportsGotoTargetsRequest;
    public Boolean supportsCancelRequest;

    @Override
    public String toString() {
        return "Capabilities{" +
            "supportsConfigurationDoneRequest=" + supportsConfigurationDoneRequest +
            ", supportsFunctionBreakpoints=" + supportsFunctionBreakpoints +
            ", supportsConditionalBreakpoints=" + supportsConditionalBreakpoints +
            ", supportsHitConditionalBreakpoints=" + supportsHitConditionalBreakpoints +
            ", supportsEvaluateForHovers=" + supportsEvaluateForHovers +
            ", supportsTerminateRequest=" + supportsTerminateRequest +
            ", supportsLogPoints=" + supportsLogPoints +
            ", supportsSteppingGranularity=" + supportsSteppingGranularity +
            ", supportsInstructionBreakpoints=" + supportsInstructionBreakpoints +
            ", supportsDisassembleRequest=" + supportsDisassembleRequest +
            ", supportsReadMemoryRequest=" + supportsReadMemoryRequest +
            ", supportsWriteMemoryRequest=" + supportsWriteMemoryRequest +
            ", supportsDataBreakpoints=" + supportsDataBreakpoints +
            ", supportsSetVariable=" + supportsSetVariable +
            ", supportsRestartRequest=" + supportsRestartRequest +
            ", supportsExceptionInfoRequest=" + supportsExceptionInfoRequest +
            ", supportsModulesRequest=" + supportsModulesRequest +
            ", supportsCompletionsRequest=" + supportsCompletionsRequest +
            ", supportsSingleThreadExecutionRequests=" + supportsSingleThreadExecutionRequests +
            ", supportsGotoTargetsRequest=" + supportsGotoTargetsRequest +
            ", supportsCancelRequest=" + supportsCancelRequest +
            '}';
    }
}
