package won.utils.blend.algorithm.astarish2;

import won.utils.blend.algorithm.BlendingInstance;

import java.util.Set;

public interface ExpansionStrategy {
    Set<SearchNode2> findSuccessors(BlendingInstance instance, AlgorithmState state, SearchNodeInspection inspection);
}
