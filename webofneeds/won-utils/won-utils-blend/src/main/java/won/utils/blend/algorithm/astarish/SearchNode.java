package won.utils.blend.algorithm.astarish;

import java.util.Arrays;
import java.util.Objects;

class SearchNode implements Comparable<SearchNode> {
    public final int score;
    public final int[] bindings;
    public final int varIndex;

    public SearchNode(int score, int[] bindings, int varIndex) {
        this.score = score;
        this.bindings = bindings;
        this.varIndex = varIndex;
    }

    @Override
    public int compareTo(SearchNode o) {
        int cmp = o.score - this.score;
        if (cmp == 0) {
            cmp = -(o.varIndex - this.varIndex);
        }
        return cmp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchNode that = (SearchNode) o;
        return varIndex == that.varIndex &&
                        Arrays.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(varIndex);
        result = 31 * result + Arrays.hashCode(bindings);
        return result;
    }
}
