package won.utils.blend.support.stats;

import java.util.Objects;

public class BindingStats {
    public final int unboundCount;
    public final int boundToConstantCount;
    public final int boundToVariableCount;

    public BindingStats(int unboundCount, int boundToConstantCount,
                    int boundToVariableCount) {
        this.unboundCount = unboundCount;
        this.boundToConstantCount = boundToConstantCount;
        this.boundToVariableCount = boundToVariableCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BindingStats that = (BindingStats) o;
        return unboundCount == that.unboundCount &&
                        boundToConstantCount == that.boundToConstantCount &&
                        boundToVariableCount == that.boundToVariableCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(unboundCount, boundToConstantCount, boundToVariableCount);
    }
}
