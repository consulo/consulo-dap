package consulo.execution.debugger.dap.protocol;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class Checksum {
    /**
     * The algorithm used to calculate this checksum.
     */
    public ChecksumAlgorithm algorithm;
    
    /**
     * Value of the checksum, encoded as a hexadecimal value.
     */
    public String checksum;

    @Override
    public String toString() {
        return "Checksum{" +
            "algorithm=" + algorithm +
            ", checksum='" + checksum + '\'' +
            '}';
    }
}
