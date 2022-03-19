package won.utils.blend.algorithm.astarish2;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

public class SearchNode2 implements Comparable<SearchNode2> {
    public final float score;
    public final int[] bindings;
    public final BitSet decided;
    public int problems;
    public int graphSize;

    public SearchNode2(float score, int[] bindings, BitSet decided, int problems, int graphSize) {
        this.score = score;
        this.bindings = bindings;
        this.decided = decided;
        this.problems = problems;
        this.graphSize = graphSize;
    }

    @Override
    public int compareTo(SearchNode2 o) {
        float scoreDiff = o.score - this.score;
        int cmp = (int) Math.signum(scoreDiff);
        if (cmp == 0) {
            cmp = this.graphSize - o.graphSize;
        }
        if (cmp == 0) {
            cmp = o.decided.cardinality() - this.decided.cardinality();
        }
        if (cmp == 0) {
            cmp = this.problems - o.problems;
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
        SearchNode2 that = (SearchNode2) o;
        return Arrays.equals(bindings, that.bindings) &&
                        Objects.equals(decided, that.decided);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(decided);
        result = 31 * result + Arrays.hashCode(bindings);
        return result;
    }

    public boolean isInitialNode() {
        return decided.nextSetBit(0) == -1;
    }

    public boolean containsCombination(int[] combination) {
        for (int i = 0; i < bindings.length; i++) {
            if (combination[i] == -1) {
                continue;
            }
            if (!decided.get(i)) {
                return false;
            }
            if (bindings[i] != combination[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean containsAnyCombination(Iterable<int[]> combinations) {
        for (int[] combination : combinations) {
            if (containsCombination(combination)) {
                return true;
            }
        }
        return false;
    }
}
