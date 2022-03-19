package won.utils.blend.algorithm.astarish2;

import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AllValuesOfNextVariableExpansionStrategy extends BaseExpansionStrategy {
    public Set<SearchNode2> findSuccessors(BlendingInstance instance, AlgorithmState state,
                    SearchNodeInspection inspection) {
        if (!(inspection.acceptable || inspection.node.isInitialNode())) {
            return Collections.emptySet();
        }
        int decideOnVariable = findUndecidedVariableWithFewestConstantOptionsIgnoringVariables(instance, state,
                        inspection);
        if (decideOnVariable == -1) {
            return Collections.emptySet();
        }
        BitSet newDecided = (BitSet) inspection.node.decided.clone();
        newDecided.set(decideOnVariable);
        Set<int[]> nextBindings = state.bindingsManager
                        .allBindingsOfVariableIncludingUnbound(inspection.node.bindings, decideOnVariable);
        Set<SearchNode2> successors = new HashSet<>();
        for (int[] newBindings : nextBindings) {
            float nextScore = estimateScore(inspection, state.bindingsManager, newBindings, newDecided);
            if (nextScore < state.maxScore) {
                continue;
            }
            SearchNode2 newSearchNode = new SearchNode2(nextScore, newBindings, newDecided, inspection.node.problems,
                            inspection.graphSize);
            successors.add(newSearchNode);
        }
        return successors;
    }

    private int findUndecidedVariableWithFewestConstantOptionsIgnoringVariables(BlendingInstance instance,
                    AlgorithmState state, SearchNodeInspection inspection) {
        int bestVarIndex = -1;
        int smallestConstantOptionCount = Integer.MAX_VALUE;
        int smallestVariableOptionCount = Integer.MAX_VALUE;
        BitSet decided = inspection.node.decided;
        for (int varIndex = 0; varIndex < state.bindingsManager.getNumberOfVariables(); varIndex++) {
            if (!decided.get(varIndex)) {
                Set<VariableBinding> options = state.bindingsManager.getOptions(varIndex);
                int variableOptionCount = (int) options.stream().filter(VariableBinding::isBoundNodeVariable).count();
                int constantOptionCount = options.size() - variableOptionCount;
                if (constantOptionCount > 0 && constantOptionCount < smallestConstantOptionCount) {
                    bestVarIndex = varIndex;
                    smallestConstantOptionCount = smallestConstantOptionCount;
                } else if (smallestConstantOptionCount == Integer.MAX_VALUE) {
                    // no variable found with options to bind to a constant
                    if (variableOptionCount > 0 && variableOptionCount < smallestVariableOptionCount) {
                        bestVarIndex = varIndex;
                        smallestVariableOptionCount = variableOptionCount;
                    }
                }
            }
        }
        return bestVarIndex;
    }
}
