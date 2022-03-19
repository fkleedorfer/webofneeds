package won.utils.blend.algorithm.sat.shacl2;

import won.utils.blend.support.bindings.VariableBinding;

import java.util.Set;

public interface ForbiddenBindings {
    boolean isForbiddenBindings(Set<VariableBinding> bindings);

    void forbidBindings(Set<VariableBinding> bindings);
}
