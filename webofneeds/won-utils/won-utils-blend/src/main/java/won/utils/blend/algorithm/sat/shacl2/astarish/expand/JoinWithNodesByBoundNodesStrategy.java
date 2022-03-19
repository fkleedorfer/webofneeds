package won.utils.blend.algorithm.sat.shacl2.astarish.expand;

import org.apache.jena.graph.Node;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.astarish.AlgorithmState;
import won.utils.blend.algorithm.sat.shacl2.astarish.ExpansionStrategy;
import won.utils.blend.algorithm.sat.shacl2.astarish.SearchNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JoinWithNodesByBoundNodesStrategy implements ExpansionStrategy {
    @Override public Set<SearchNode> findSuccessors(BlendingInstance instance, AlgorithmState state, SearchNode node) {
        List<SearchNode> preliminaryResults = new ArrayList<>();
        preliminaryResults.add(node);
        for (Node boundNode : node.encounteredVariables.getVariables()){
            List<SearchNode> nextCandidates = state.searchNodesByBoundNode.get(boundNode);
            List<SearchNode> candidatesUsedUp = new ArrayList<>();
            if (nextCandidates == null){
                continue;
            }
            List<SearchNode> freshlyJoined = new ArrayList<>();
            for (SearchNode preliminaryResult: preliminaryResults) {
                boolean noJoinAtAll = true;
                for (SearchNode currentCandidate: nextCandidates) {
                    Optional<SearchNode> currentJoin = preliminaryResult.outerJoin(currentCandidate);
                    if (currentJoin.isPresent()) {
                        noJoinAtAll = false;
                        freshlyJoined.add(currentJoin.get());
                        candidatesUsedUp.add(currentCandidate);
                    }
                }
                if (noJoinAtAll) {
                    freshlyJoined.add(preliminaryResult);
                }
            }
            preliminaryResults = freshlyJoined;
            nextCandidates.removeAll(candidatesUsedUp);
        }
        return preliminaryResults.stream().collect(Collectors.toSet());
    }
}
