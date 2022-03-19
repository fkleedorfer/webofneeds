package won.utils.blend.algorithm.sat.shacl2.forbiddenbindings;

import won.utils.blend.algorithm.sat.shacl2.astarish.AlgorithmState;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.index.CompactBindingsIndex;

import java.util.Set;

public class CompactForbiddenBindings implements won.utils.blend.algorithm.sat.shacl2.ForbiddenBindings {
    final AlgorithmState state;
    final CompactBindingsIndex index;

    public CompactForbiddenBindings(AlgorithmState state) {
        this.state = state;
        this.index = new CompactBindingsIndex(state.bindingsManager.getOptionCountPerVariable());
    }

    @Override
    public boolean isForbiddenBindings(Set<VariableBinding> bindings) {
        // List<int[]> all = this.intArrayIndex.toList();
        int size = this.index.size();
        int[] bindingsarray = state.bindingsManager.fromBindings(bindings).copyIndexArray();
        return this.index.contains(bindingsarray);
    }

    @Override
    public void forbidBindings(Set<VariableBinding> bindings) {
        int size = this.index.size();
        int[] bindingsarray = state.bindingsManager.fromBindings(bindings).copyIndexArray();
        this.index.put(bindingsarray);
        size = this.index.size();
    }
}
