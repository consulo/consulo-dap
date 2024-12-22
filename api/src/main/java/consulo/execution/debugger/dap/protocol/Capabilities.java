package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-21
 */
public class Capabilities {
    /**
     * The debug adapter supports the `configurationDone` request.
     */
    public Boolean supportsConfigurationDoneRequest;
    /**
     * The debug adapter supports function breakpoints.
     */
    public Boolean supportsFunctionBreakpoints;
    /**
     * The debug adapter supports conditional breakpoints.
     */
    public Boolean supportsConditionalBreakpoints;
    /**
     * The debug adapter supports breakpoints that break execution after a
     * specified number of hits.
     */
    public Boolean supportsHitConditionalBreakpoints;
    /**
     * The debug adapter supports a (side effect free) `evaluate` request for data
     * hovers.
     */
    public Boolean supportsEvaluateForHovers;
    /**
     * The debug adapter supports the `terminate` request.
     */
    public Boolean supportsTerminateRequest;

    @Override
    public String toString() {
        return "Capabilities{" +
            "supportsConfigurationDoneRequest=" + supportsConfigurationDoneRequest +
            ", supportsFunctionBreakpoints=" + supportsFunctionBreakpoints +
            ", supportsConditionalBreakpoints=" + supportsConditionalBreakpoints +
            ", supportsHitConditionalBreakpoints=" + supportsHitConditionalBreakpoints +
            ", supportsEvaluateForHovers=" + supportsEvaluateForHovers +
            ", supportsTerminateRequest=" + supportsTerminateRequest +
            '}';
    }
}
