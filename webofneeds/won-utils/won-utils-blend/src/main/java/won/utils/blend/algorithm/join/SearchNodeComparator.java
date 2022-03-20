package won.utils.blend.algorithm.join;

import java.util.Comparator;

import static won.utils.blend.algorithm.join.AlgorithmStateLogic.*;

public class SearchNodeComparator implements Comparator<SearchNode> {
    private AlgorithmState state;
    private Comparator<SearchNode> comparator;

    public SearchNodeComparator(AlgorithmState state) {
        this.state = state;
        this.comparator = Comparator
                        .comparing((SearchNode n) -> {
                            int unexplored = countUnexploredVariables(state, n.encounteredVariablesFlat);
                            int explored = countExploredVariables(state, n.encounteredVariablesFlat);
                            // make sure a node with explored variables is ranked lower than any number of
                            // unexplored
                            return -unexplored - (state.allVariables.size() + 1) * explored;
                        })
                        .thenComparing((SearchNode n) -> -n.encounteredVariablesFlat.size())
                        .thenComparing((SearchNode n) -> n.id);
    }

    private int map0toMaxInt(int unex) {
        if (unex == 0) {
            return Integer.MAX_VALUE;
        } else {
            return unex;
        }
    }

    @Override
    public int compare(SearchNode left, SearchNode right) {
        return comparator.compare(left, right);
    }
}
