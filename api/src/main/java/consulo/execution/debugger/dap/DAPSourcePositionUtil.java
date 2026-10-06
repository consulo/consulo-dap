package consulo.execution.debugger.dap;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.XSourcePositionFactory;
import consulo.execution.debugger.dap.protocol.Source;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-06
 */
public final class DAPSourcePositionUtil {
    private DAPSourcePositionUtil() {
    }

    @Nullable
    public static XSourcePosition createPosition(@Nullable Source source, int line, int column) {
        if (source == null || source.path == null || line < 0) {
            return null;
        }
        String path = source.path;
        return ReadAction.compute(() -> {
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
            if (file == null) {
                return null;
            }
            return Application.get().getInstance(XSourcePositionFactory.class).createPosition(file, line, Math.max(column, 0));
        });
    }
}
