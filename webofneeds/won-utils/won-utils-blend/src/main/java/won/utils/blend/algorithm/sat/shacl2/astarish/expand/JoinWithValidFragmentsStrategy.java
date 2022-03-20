package won.utils.blend.algorithm.sat.shacl2.astarish.expand;

import org.apache.jena.graph.Node;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.astarish.AlgorithmState;
import won.utils.blend.algorithm.sat.shacl2.astarish.ExpansionStrategy;
import won.utils.blend.algorithm.sat.shacl2.astarish.SearchNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JoinWithValidFragmentsStrategy implements ExpansionStrategy {
    @Override
    public Set<SearchNode> findSuccessors(BlendingInstance instance, AlgorithmState state, SearchNode node) {
        if (node.encounteredVariables.isEmpty()) {
            return Collections.emptySet();
        }
        if (state.validFragments.isEmpty()) {
            return Collections.emptySet();
        }
        Set<SearchNode> expanded = new HashSet<>();
        Set<SearchNode> obsoleteFragments = new HashSet<>();
        for (Node var : node.encounteredVariables.getVariables()) {
            Set<SearchNode> joinNodes = expanded.isEmpty() ? Collections.singleton(node) : expanded;
            Set<SearchNode> joinedNodes = new HashSet<>();
            for (SearchNode validFragment : state.validFragments) {
                if (validFragment.bindings.isBound(var)) {
                    for (SearchNode toJoin : joinNodes) {
                        Optional<SearchNode> joined = toJoin.outerJoin(validFragment);
                        if (joined.isPresent()) {
                            joinedNodes.add(joined.get());
                            obsoleteFragments.add(validFragment);
                        }
                    }
                }
            }
            expanded = joinedNodes;
        }
        state.validFragments.removeAll(obsoleteFragments);
        return expanded;
    }
}
