package won.utils.blend.algorithm.sat.shacl2.astarish.expand;

import org.apache.jena.graph.Node;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.ShaclValidator;
import won.utils.blend.algorithm.sat.shacl2.astarish.AlgorithmState;
import won.utils.blend.algorithm.sat.shacl2.astarish.ExpansionStrategy;
import won.utils.blend.algorithm.sat.shacl2.astarish.SearchNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JoinWithOpenNodesStrategy implements ExpansionStrategy {
    @Override
    public Set<SearchNode> findSuccessors(BlendingInstance instance, AlgorithmState state, SearchNode node) {
        if (node.encounteredVariables.isEmpty()) {
            return Collections.emptySet();
        }
        if (state.open.isEmpty() && state.validFragments.isEmpty()) {
            return Collections.emptySet();
        }
        Set<SearchNode> joinCandidates = new HashSet<>(state.open);
        joinCandidates.addAll(state.validFragments);
        Set<SearchNode> expanded = new HashSet<>();
        Set<SearchNode> usedNodes = new HashSet<>();
        for (Node var : node.encounteredVariables.getVariables()) {
            Set<SearchNode> joinNodes = expanded.isEmpty() ? Collections.singleton(node) : expanded;
            Set<SearchNode> joinedNodes = new HashSet<>();
            for (SearchNode openNode : joinCandidates) {
                if (openNode.bindings.isBound(var)) {
                    for (SearchNode toJoin : joinNodes) {
                        Optional<SearchNode> joined = toJoin.outerJoin(openNode);
                        if (joined.isPresent()) {
                            joinedNodes.add(joined.get());
                            usedNodes.add(openNode);
                        }
                    }
                }
            }
            expanded = joinedNodes;
        }
        expanded = expanded
                        .stream()
                        .map(searchNode -> ShaclValidator.validateSearchNode(instance, state, searchNode))
                        .filter(searchNode -> !searchNode.valid.isFalse())
                        .collect(Collectors.toSet());
        ;
        state.open.removeAll(usedNodes);
        state.validFragments.removeAll(usedNodes);
        return expanded;
    }
}
