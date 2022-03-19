package won.utils.blend.algorithm.astarish2;

import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;

import java.util.BitSet;

public interface ScoreEstimator {
    float estimate(SearchNodeInspection inspection, CompactBindingsManager bindingsManager, int[] bindings,
                    BitSet decided);
}
