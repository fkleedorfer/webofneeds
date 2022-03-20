package won.utils.blend.algorithm.sat.shacl2.astarish;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.sat.shacl2.BindingValidationResult;
import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.algorithm.support.bindings.CompactBindingsManager;
import won.utils.blend.algorithm.support.bindings.CompactVariableBindings;
import won.utils.blend.algorithm.support.bindings.CompactVariables;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.*;

public class SearchNode implements Comparable<SearchNode> {
    public final Set<Shape> shapes;
    public final Set<Node> focusNodes;
    public final CompactVariableBindings bindings;
    public final CompactVariables encounteredVariables;
    public final Ternary valid;
    public final Ternary globallyValid;

    public SearchNode(Set<Shape> shapes, Set<Node> focusNodes, CompactVariableBindings bindings,
                    CompactVariables encounteredVariables,
                    Ternary valid, Ternary globallyValid) {
        this.shapes = shapes;
        this.focusNodes = focusNodes;
        this.bindings = bindings;
        this.encounteredVariables = encounteredVariables;
        this.valid = valid;
        this.globallyValid = globallyValid;
    }

    public static Optional<SearchNode> of(CompactBindingsManager bindingsManager,
                    BindingValidationResult... bindingValidationResults) {
        Set<Node> focusNodes = new HashSet<>();
        Set<Shape> shapes = new HashSet<>();
        VariableBindings bindings = null;
        Set<Node> encounteredVariables = new HashSet<>();
        Ternary valid = null;
        Ternary globallyValid = null;
        for (int i = 0; i < bindingValidationResults.length; i++) {
            if (bindings == null) {
                bindings = bindingValidationResults[i].bindings;
            } else if (!bindings.equals(bindingValidationResults[i].bindings)) {
                throw new IllegalArgumentException(
                                "Cannot make SearchNode of multiple BindingValidationResults with different bindings");
            }
            if (valid == null) {
                valid = bindingValidationResults[i].valid;
            }
            Ternary currentValid = bindingValidationResults[i].valid;
            if (!bindingValidationResults[i].encounteredVariables.isEmpty()) {
                currentValid = Ternary.UNKNOWN;
            }
            valid = valid.and(currentValid);
            if (globallyValid == null) {
                globallyValid = bindingValidationResults[i].globallyValid;
            }
            globallyValid = globallyValid.and(bindingValidationResults[i].globallyValid);
            focusNodes.add(bindingValidationResults[i].node);
            shapes.add(bindingValidationResults[i].shape);
            encounteredVariables.addAll(bindingValidationResults[i].encounteredVariables);
        }
        if (valid.isFalse()) {
            return Optional.empty();
        }
        return Optional.of(new SearchNode(
                        shapes,
                        focusNodes,
                        bindingsManager.fromBindings(bindings.getBindingsAsSet()),
                        bindingsManager.getCompactVariables(encounteredVariables),
                        valid,
                        globallyValid));
    }

    public SearchNode globallyValid(boolean globallyValid) {
        return new SearchNode(shapes, focusNodes, bindings, encounteredVariables, valid, Ternary.of(globallyValid));
    }

    public Optional<SearchNode> merge(SearchNode other) {
        if (!bindings.equals(other.bindings)) {
            return Optional.empty();
        }
        return Optional.ofNullable(combine(other));
    }

    public Optional<SearchNode> join(SearchNode other) {
        if (!bindings.overlapsWith(other.bindings)) {
            return Optional.empty();
        }
        SearchNode sn = combine(other);
        return Optional.ofNullable(sn);
    }

    public Optional<SearchNode> outerJoin(SearchNode other) {
        if (bindings.conflictsWith(other.bindings)) {
            return Optional.empty();
        }
        SearchNode sn = combine(other);
        return Optional.ofNullable(sn);
    }

    private SearchNode combine(SearchNode other) {
        Set<Shape> mergedShapes = new HashSet<>(shapes);
        mergedShapes.addAll(other.shapes);
        Set<Node> mergedFocusNodes = new HashSet<>(focusNodes);
        mergedFocusNodes.addAll(other.focusNodes);
        CompactVariableBindings mergedBindings = bindings.mergeWith(other.bindings);
        CompactVariables mergedEncounteredVariables = encounteredVariables.mergeWith(other.encounteredVariables)
                        .removeIfBound(mergedBindings);
        Ternary mergedValid = Ternary.UNKNOWN;
        Ternary mergedGloballyValid = Ternary.UNKNOWN;
        return new SearchNode(mergedShapes,
                        mergedFocusNodes,
                        mergedBindings,
                        mergedEncounteredVariables,
                        mergedValid,
                        mergedGloballyValid);
    }

    public boolean equalsIgnoreValidity(SearchNode o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return shapes.equals(o.shapes) && bindings.equals(o.bindings)
                        && encounteredVariables.equals(o.encounteredVariables);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SearchNode that = (SearchNode) o;
        return shapes.equals(that.shapes) && bindings.equals(that.bindings)
                        && encounteredVariables.equals(that.encounteredVariables)
                        && valid == that.valid && globallyValid == that.globallyValid;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(shapes, valid, globallyValid);
        result = 31 * result + bindings.hashCode();
        result = 31 * result + encounteredVariables.hashCode();
        return result;
    }

    @Override
    public int compareTo(SearchNode o) {
        if (this.equals(o)) {
            return 0;
        }
        int cmp = 0;
        if (cmp == 0) {
            cmp = this.globallyValid.compareTo(o.globallyValid);
        }
        if (cmp == 0) {
            cmp = this.encounteredVariables.size() - o.encounteredVariables.size();
        }
        if (cmp == 0) {
            cmp = this.valid.compareTo(o.valid);
        }
        if (cmp == 0) {
            cmp = this.bindings.size() - o.bindings.size();
        }
        if (cmp == 0) {
            cmp = this.shapes.size() - o.shapes.size();
        }
        return cmp;
    }

    @Override
    public String toString() {
        return "SearchNode{" +
                        "bindings=" + bindings.toString() +
                        ", encounteredVariables=" + encounteredVariables.toString() +
                        ", valid=" + valid +
                        ", globallyValid=" + globallyValid +
                        '}';
    }
}
