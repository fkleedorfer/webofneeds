package won.utils.blend.algorithm.astarish2;

import won.utils.blend.algorithm.BlendingInstance;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CombinedExpansionStrategy implements ExpansionStrategy {
    private final List<ExpansionStrategy> strategies;

    public CombinedExpansionStrategy(List<ExpansionStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toList());
    }

    public CombinedExpansionStrategy(ExpansionStrategy... strategies) {
        this.strategies = Arrays.asList(strategies);
    }

    @Override
    public Set<SearchNode2> findSuccessors(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        for (ExpansionStrategy strategy : strategies) {
            Set<SearchNode2> expanded = strategy.findSuccessors(instance, state, inspection);
            if (expanded != null && !expanded.isEmpty()) {
                return expanded;
            }
        }
        return Collections.emptySet();
    }
}
