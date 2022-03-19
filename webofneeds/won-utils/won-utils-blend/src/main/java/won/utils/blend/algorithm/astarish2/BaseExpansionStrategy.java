package won.utils.blend.algorithm.astarish2;

import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;

import java.util.BitSet;

public abstract class BaseExpansionStrategy implements ExpansionStrategy {
    private ScoreEstimator scoreEstimator;

    public BaseExpansionStrategy(ScoreEstimator scoreEstimator) {
        this.scoreEstimator = scoreEstimator;
    }

    public BaseExpansionStrategy() {
        this.scoreEstimator = new VarConstCountEstimator();
    }

    protected float estimateScore(SearchNodeInspection inspection,
                    CompactBindingsManager bindingsManager, int[] bindings,
                    BitSet decided) {
        return scoreEstimator.estimate(inspection, bindingsManager, bindings, decided);
    }
}
