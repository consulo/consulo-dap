package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public class LocationsArguments {
    public int locationReference;

    public LocationsArguments(int locationReference) {
        this.locationReference = locationReference;
    }
}
