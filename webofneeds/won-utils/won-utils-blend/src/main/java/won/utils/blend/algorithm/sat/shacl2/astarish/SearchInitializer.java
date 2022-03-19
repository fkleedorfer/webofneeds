package won.utils.blend.algorithm.sat.shacl2.astarish;

import won.utils.blend.algorithm.BlendingInstance;

import java.util.Set;

public interface SearchInitializer {
    public Set<SearchNode> initializeSearch(BlendingInstance instance, AlgorithmState state);
}
