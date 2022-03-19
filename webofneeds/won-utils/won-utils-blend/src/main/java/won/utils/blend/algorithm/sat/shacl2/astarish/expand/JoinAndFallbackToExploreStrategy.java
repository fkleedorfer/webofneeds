package won.utils.blend.algorithm.sat.shacl2.astarish.expand;

import org.apache.jena.graph.Node;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.astarish.*;

import java.util.Collections;
import java.util.Set;

public class JoinAndFallbackToExploreStrategy implements ExpansionStrategy {
    private ExpansionStrategy joinNodesStrategy = new JoinWithNodesByBoundNodesStrategy();
    private ExpansionStrategy exploreAllOptionsStrategy = new ExploreAllOptionsStrategy();
    @Override public Set<SearchNode> findSuccessors(BlendingInstance instance, AlgorithmState state, SearchNode node) {
        try {
            state.debugWriter.incIndent();
            state.log.trace(() -> "trying to join node with valid fragments on one or more of its encountered variables");
            Set<SearchNode> expanded = joinNodesStrategy.findSuccessors(instance,state, node);
            expanded.remove(node);
            if (!expanded.isEmpty()){
                state.log.trace(() -> "join successful");
                printExpanded(state, expanded);
                return expanded;
            }
            state.log.trace(() -> "failed, no joins possible");
            Set<Node> encounteredVars = node.encounteredVariables.getVariables();
            if (encounteredVars.stream().anyMatch(v -> state.allOptionsExplored.contains(v))) {
                return Collections.emptySet();
            }
            state.allOptionsExplored.addAll(encounteredVars);
            state.log.trace(() -> "trying all options for the encountered variables");
            expanded = exploreAllOptionsStrategy.findSuccessors(instance, state, node);
            printExpanded(state, expanded);
            return expanded;
        } finally {
            state.debugWriter.decIndent();
        }
    }

    private void printExpanded(AlgorithmState state, Set<SearchNode> expanded) {
        state.log.info(() -> "expansion result:");
        state.debugWriter.incIndent();
        for(SearchNode node: expanded) {
            state.log.info(() -> "expanded node:");
            state.debugWriter.incIndent();
            state.log.info(() -> new VerbosityAwareSearchNodeFormatter(state).format(node, state.bindingsManager));
            state.debugWriter.decIndent();
        }
        state.debugWriter.decIndent();
    }
}
