package won.utils.blend.algorithm.astarish2;

import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.BitSet;
import java.util.Set;

public class VarConstCountEstimator implements ScoreEstimator {
    @Override
    public float estimate(SearchNodeInspection inspection, CompactBindingsManager bindingsManager, int[] bindings,
                    BitSet decided) {
        return scoreBoundVariables(bindingsManager.toBindings(bindings))
                        + bindingsManager.getNumberOfVariables()
                        - decided.cardinality();
    }

    private float scoreBoundVariables(Set<VariableBinding> bindings) {
        int boundToVariables = (int) bindings.stream().filter(VariableBinding::isBoundNodeVariable).count();
        int boundToConstants = bindings.size() - boundToVariables;
        return -.5f * boundToVariables + boundToConstants;
    }
}
