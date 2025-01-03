package consulo.execution.debugger.dap;

/**
 * @author VISTALL
 * @since 2025-01-04
 */
public interface SourceCodeMapper {
    SourceCodeMapper ZERO_BASED = new SourceCodeMapper() {
        @Override
        public int toDAP(int positionFromConsulo) {
            return positionFromConsulo;
        }

        @Override
        public int fromDAP(int positionFromDAP) {
            return positionFromDAP;
        }
    };

    SourceCodeMapper ONE_BASED = new SourceCodeMapper() {
        @Override
        public int toDAP(int positionFromConsulo) {
            return positionFromConsulo + 1;
        }

        @Override
        public int fromDAP(int positionFromDAP) {
            return positionFromDAP - 1;
        }
    };

    int toDAP(int positionFromConsulo);

    int fromDAP(int positionFromDAP);
}
