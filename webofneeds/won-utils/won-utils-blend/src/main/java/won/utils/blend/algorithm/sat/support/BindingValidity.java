package won.utils.blend.algorithm.sat.support;

import won.utils.blend.support.bindings.VariableBinding;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BindingValidity implements Comparable<BindingValidity> {
    public Ternary valid;
    public Set<VariableBinding> bindings;

    public BindingValidity(Ternary valid, Set<VariableBinding> bindings) {
        this.valid = valid;
        this.bindings = bindings;
    }

    @Override
    public String toString() {
        return "BindingValidity{" +
                        "valid=" + valid +
                        ", bindings=" + bindings +
                        '}';
    }

    @Override
    public int compareTo(BindingValidity o) {
        if (o == null) {
            return -1;
        }
        int cmp = 0;
        List<VariableBinding> thisBindings = this.bindings.stream().sorted().collect(Collectors.toList());
        List<VariableBinding> otherBindings = o.bindings.stream().sorted().collect(Collectors.toList());
        for (int ind = 0; ind < Math.min(thisBindings.size(), otherBindings.size()); ind++) {
            cmp = thisBindings.get(ind).compareTo(otherBindings.get(ind));
            if (cmp != 0) {
                return cmp;
            }
        }
        cmp = thisBindings.size() - otherBindings.size();
        if (cmp != 0) {
            return cmp;
        }
        return this.valid.compareTo(o.valid);
    }
}
