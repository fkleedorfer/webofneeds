package won.utils.blend.algorithm.sat.shacl2.astarish;

import won.utils.blend.algorithm.BlendingInstance;

import java.util.Set;

public interface ExpansionStrategy {
    Set<SearchNode> findSuccessors(BlendingInstance instance, AlgorithmState state, SearchNode node);
}
