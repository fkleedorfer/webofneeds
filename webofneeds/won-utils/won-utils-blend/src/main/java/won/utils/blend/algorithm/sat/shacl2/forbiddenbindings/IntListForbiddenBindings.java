package won.utils.blend.algorithm.sat.shacl2.forbiddenbindings;

import won.utils.blend.algorithm.sat.shacl2.ForbiddenBindings;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;
import won.utils.blend.algorithm.support.bindings.CompactVariableBindings;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntListForbiddenBindings implements ForbiddenBindings {
    private Set<List<Integer>> forbiddenBindings = new HashSet<>();
    private CompactBindingsManager bindingsManager;
    public IntListForbiddenBindings(CompactBindingsManager manager) {
        bindingsManager = manager;
    }

    @Override public boolean isForbiddenBindings(Set<VariableBinding> bindings) {
        List<Integer> bindingsIndices = bindingsManager.fromBindings(bindings).indexList();
        return isForbiddenBindings(bindingsIndices);
    }

    public boolean isForbiddenBindings(List<Integer> bindingsIndices) {
        if (true){
            return false;
        }
        return forbiddenBindings.stream().anyMatch(f -> {
            boolean allZeros = true;
            for (int i = 0; i < bindingsIndices.size(); i++) {
                int forbiddenValue = f.get(i);
                if (forbiddenValue > 0) {
                    allZeros = false;
                    if (bindingsIndices.get(i) != forbiddenValue) {
                        return false;
                    }
                }
            }
            // if the forbidden bindings array is all-0, it forbids nothing
            return !allZeros;
        });
    }

    @Override public void forbidBindings(Set<VariableBinding> bindings) {
        if (true) {
            return;
        }
        CompactVariableBindings compacted = bindingsManager.fromBindings(bindings);
        forbiddenBindings.add(compacted.indexList());
    }
}
