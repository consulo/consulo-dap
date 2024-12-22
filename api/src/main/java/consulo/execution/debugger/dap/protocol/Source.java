package consulo.execution.debugger.dap.protocol;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2024-12-22
 */
public class Source {
    /**
     * The short name of the source. Every source returned from the debug adapter
     * has a name.
     * When sending a source to the debug adapter this name is optional.
     */
    public String name;

    /**
     * The path of the source to be shown in the UI.
     * It is only used to locate and load the content of the source if no
     * `sourceReference` is specified (or its value is 0).
     */
    public String path;

    /**
     * If the value > 0 the contents of the source must be retrieved through the
     * `source` request (even if a path is specified).
     * Since a `sourceReference` is only valid for a session, it can not be used
     * to persist a source.
     * The value should be less than or equal to 2147483647 (2^31-1).
     */
    public Integer sourceReference;

    /**
     * A hint for how to present the source in the UI.
     * A value of `deemphasize` can be used to indicate that the source is not
     * available or that it is skipped on stepping.
     * Values: 'normal', 'emphasize', 'deemphasize'
     */
    public String presentationHint;

    /**
     * The origin of this source. For example, 'internal module', 'inlined content
     * from source map', etc.
     */
    public String origin;

    /**
     * A list of sources that are related to this source. These may be the source
     * that generated this source.
     */
    public Source[] sources;

    /**
     * Additional data that a debug adapter might want to loop through the client.
     * The client should leave the data intact and persist it across sessions. The
     * client should not interpret the data.
     */
    public Object adapterData;

    /**
     * The checksums associated with this file.
     */
    public Checksum[] checksums;

    @Override
    public String toString() {
        return "Source{" +
            "name='" + name + '\'' +
            ", path='" + path + '\'' +
            ", sourceReference=" + sourceReference +
            ", presentationHint='" + presentationHint + '\'' +
            ", origin='" + origin + '\'' +
            ", sources=" + (sources == null ? null : Arrays.asList(sources)) +
            ", adapterData=" + adapterData +
            ", checksums=" + (checksums == null ? null : Arrays.asList(checksums)) +
            '}';
    }
}
