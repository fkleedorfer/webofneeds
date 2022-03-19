package won.utils.blend.algorithm.sat.shacl2.forbiddenbindings;

import won.utils.blend.BLEND;
import won.utils.blend.algorithm.sat.shacl2.ForbiddenBindings;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class VariableBindingSetForbiddenBindings implements ForbiddenBindings {
    private Set<Set<VariableBinding>> forbiddenBindings = new HashSet<>();

    @Override
    public boolean isForbiddenBindings(Set<VariableBinding> bindings) {
        Set<VariableBinding> filtered = removeUnbound(bindings);
        return forbiddenBindings.stream().anyMatch(f -> filtered.containsAll(f));
    }

    public Set<VariableBinding> removeUnbound(Set<VariableBinding> bindings) {
        return bindings.stream().filter(b -> b.getBoundNode().equals(BLEND.unbound)).collect(toSet());
    }

    @Override
    public void forbidBindings(Set<VariableBinding> bindings) {
        Set<VariableBinding> filtered = removeUnbound(bindings);
        if (!filtered.isEmpty()) {
            forbiddenBindings.add(filtered);
        }
    }
}
