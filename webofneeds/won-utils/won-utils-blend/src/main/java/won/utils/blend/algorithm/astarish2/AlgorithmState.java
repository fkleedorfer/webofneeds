package won.utils.blend.algorithm.astarish2;

import org.apache.jena.graph.Node;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;
import won.utils.blend.algorithm.support.bindings.CompactVariables;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

class AlgorithmState {
    final Queue<SearchNode2> open = new PriorityQueue<>();
    final Set<SearchNode2> closed = new HashSet<>();
    final Set<SearchNode2> results = new HashSet<>();
    final CompactBindingsManager bindingsManager;
    float maxScore = -1;
    int minGraphSize = Integer.MAX_VALUE;
    int nodesInspected = 0;
    int nodesPruned = 0;
    final Set<int[]> forbiddenCombinations = new HashSet<>();

    public AlgorithmState(CompactBindingsManager bindingsManager) {
        this.bindingsManager = bindingsManager;
    }
}
