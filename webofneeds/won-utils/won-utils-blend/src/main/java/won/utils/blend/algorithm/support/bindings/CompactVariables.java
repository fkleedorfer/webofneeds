package won.utils.blend.algorithm.support.bindings;

import org.apache.jena.graph.Node;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public class CompactVariables {
    final int[] variables;
    private CompactBindingsManager compactBindingsManager;

    public CompactVariables(int[] variables,
                    CompactBindingsManager compactBindingsManager) {
        this.variables = variables;
        this.compactBindingsManager = compactBindingsManager;
    }

    public Set<Node> getVariables() {
        return this.compactBindingsManager.getVariables(variables);
    }

    public CompactVariables mergeWith(CompactVariables other) {
        if (this.compactBindingsManager == other.compactBindingsManager) {
            int[] merged = ArrayUtils.mergeArrays(this.variables, other.variables);
            return new CompactVariables(merged, compactBindingsManager);
        } else {
            Set<Node> vars = getVariables();
            vars.addAll(other.getVariables());
            return compactBindingsManager.getCompactVariables(vars);
        }
    }

    public boolean hasManager(CompactBindingsManager manager) {
        return this.compactBindingsManager.equals(manager);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CompactVariables that = (CompactVariables) o;
        return Arrays.equals(variables, that.variables) && compactBindingsManager.equals(that.compactBindingsManager);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(compactBindingsManager);
        result = 31 * result + Arrays.hashCode(variables);
        return result;
    }

    public int size() {
        return variables.length;
    }

    @Override
    public String toString() {
        return getVariables().toString();
    }

    public boolean isEmpty() {
        return variables == null || variables.length == 0;
    }

    public CompactVariables removeIfBound(CompactVariableBindings mergedBindings) {
        if (mergedBindings.hasManager(this.compactBindingsManager)) {
            return compactBindingsManager.removeFromVariablesIfBound(this.variables, mergedBindings.bindings);
        }
        return compactBindingsManager.removeFromVariablesIfBound(this.variables, mergedBindings.getBindingsAsSet());
    }
}
