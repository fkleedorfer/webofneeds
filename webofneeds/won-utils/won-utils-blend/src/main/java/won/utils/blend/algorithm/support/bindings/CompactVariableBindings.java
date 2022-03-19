package won.utils.blend.algorithm.support.bindings;

import org.apache.jena.graph.Node;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CompactVariableBindings {
    private CompactBindingsManager compactBindingsManager;
    final int[] bindings;

    CompactVariableBindings(int[] bindingsArray, CompactBindingsManager compactBindingsManager) {
        this.bindings = bindingsArray;
        this.compactBindingsManager = compactBindingsManager;
    }

    public Set<VariableBinding> getBindingsAsSet(){
        return compactBindingsManager.toBindings(bindings);
    }
    public VariableBindings getVariableBindings(){
        return new VariableBindings(compactBindingsManager.getVariables(), getBindingsAsSet());
    }
    public List<Integer> indexList(){
        return Arrays.stream(bindings).boxed().collect(Collectors.toList());
    }

    public int[] copyIndexArray(){
        int[] copy = new int[bindings.length];
        System.arraycopy(bindings, 0, copy, 0, bindings.length);
        return copy;
    }

    public boolean overlapsWith(CompactVariableBindings other) {
        if (this.compactBindingsManager == other.compactBindingsManager) {
            return this.compactBindingsManager.isBindingsOverlap(this.bindings, other.bindings);
        } else {
            VariableBindings ours = getVariableBindings();
            VariableBindings theirs = other.getVariableBindings();
            return ours.overlapsWith(theirs);
        }
    }

    public boolean conflictsWith(CompactVariableBindings other) {
        if (this.compactBindingsManager == other.compactBindingsManager) {
            return this.compactBindingsManager.isBindingsConflict(this.bindings, other.bindings);
        } else {
            VariableBindings ours = getVariableBindings();
            VariableBindings theirs = other.getVariableBindings();
            return ours.conflictsWith(theirs);
        }
    }

    public boolean isAllUnbound(){
        return compactBindingsManager.isAllUnbound(bindings);
    }
    public boolean equals(CompactVariableBindings other) {
        if (other == null) return false;
        if (other == this) return true;
        if (other.compactBindingsManager == this.compactBindingsManager ) {
            return Arrays.equals(this.bindings, other.bindings);
        }
        return getVariableBindings().equals(other.getVariableBindings());
    }

    public boolean hasManager(CompactBindingsManager manager) {
        return this.compactBindingsManager.equals(manager);
    }

    public CompactVariableBindings mergeWith(CompactVariableBindings other) {
        if (this.compactBindingsManager == other.compactBindingsManager) {
            return new CompactVariableBindings(ArrayUtils.combineArrays(this.bindings, other.bindings), compactBindingsManager);
        } else {
            return compactBindingsManager.fromBindings(getVariableBindings().mergeWith(other.getVariableBindings()).getBindingsAsSet());
        }
    }
    public int size() {
        int count = 0;
        for (int i = 0; i < bindings.length ; i++ ) {
            if (bindings[i] > 0) {
                count++;
            }
        }
        return count;
    }


    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CompactVariableBindings that = (CompactVariableBindings) o;
        return compactBindingsManager.equals(that.compactBindingsManager) && Arrays.equals(bindings,
                        that.bindings);
    }

    @Override public int hashCode() {
        int result = Objects.hash(compactBindingsManager);
        result = 31 * result + Arrays.hashCode(bindings);
        return result;
    }

    @Override public String toString() {
        return getVariableBindings().toString();
    }

    public boolean isAnyVariableBound(CompactVariables encounteredVariables) {
        if (encounteredVariables.hasManager(this.compactBindingsManager)){
            this.compactBindingsManager.isAnyVariableBound(this.bindings, encounteredVariables.variables);
        }
        return this.compactBindingsManager.isAnyVariableBound(this.bindings, encounteredVariables.getVariables());
    }

    public boolean isBound(Node variable) {
        return this.compactBindingsManager.isBoundVariable(this.bindings, variable);
    }

    public boolean isEmpty() {
        return compactBindingsManager.isAllUnbound(this.bindings);
    }
}
