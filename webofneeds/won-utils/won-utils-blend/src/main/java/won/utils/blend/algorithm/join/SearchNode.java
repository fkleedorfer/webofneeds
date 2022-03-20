package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static won.utils.blend.algorithm.join.SearchNodeLogic.getPriority;

public class SearchNode implements Comparable<SearchNode> {
    public final Set<SearchNode> predecessors;
    private static final AtomicLong idCounter = new AtomicLong(0);
    public final AlgorithmState state;
    public final long id = idCounter.incrementAndGet();
    public boolean invalid = false;
    public final VariableBindings bindings;
    public final Set<Shape> untestedShapes = new HashSet<>();
    public final Set<Set<Node>> encounteredVariables = new HashSet<>();
    public final Set<Node> encounteredVariablesFlat = new HashSet<>();
    public final Map<Node, Set<Shape>> unsatisfiedShapesByRequiredVariable = new HashMap<>();
    public final Set<Shape> satisfiedShapes = new HashSet<>();
    public final Set<Node> exploring = new HashSet<>();
    public Shape invalidShape;

    public SearchNode(AlgorithmState state, Set<Node> variables) {
        this.state = state;
        this.bindings = new VariableBindings(variables);
        this.predecessors = Collections.emptySet();
    }

    public SearchNode(AlgorithmState state, SearchNode toCopy) {
        this.state = state;
        this.bindings = new VariableBindings(toCopy.bindings);
        this.untestedShapes.addAll(toCopy.untestedShapes);
        toCopy.encounteredVariables.stream().forEach(vars -> this.encounteredVariables.add(new HashSet<>(vars)));
        this.encounteredVariablesFlat.addAll(toCopy.encounteredVariablesFlat);
        toCopy.unsatisfiedShapesByRequiredVariable.entrySet().stream().forEach(
                        e -> this.unsatisfiedShapesByRequiredVariable.put(e.getKey(), new HashSet<>(e.getValue())));
        this.satisfiedShapes.addAll(toCopy.satisfiedShapes);
        this.invalidShape = toCopy.invalidShape;
        this.exploring.addAll(toCopy.exploring);
        this.predecessors = Set.of(toCopy);
    }

    public SearchNode(SearchNode left, SearchNode right) {
        this.state = left.state;
        this.bindings = new VariableBindings(left.bindings.getVariables());
        this.predecessors = Set.of(left, right);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SearchNode that = (SearchNode) o;
        return bindings.equals(that.bindings)
                        && encounteredVariablesFlat.equals(that.encounteredVariablesFlat)
                        && unsatisfiedShapesByRequiredVariable.equals(that.unsatisfiedShapesByRequiredVariable)
                        && untestedShapes.equals(that.untestedShapes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindings, encounteredVariablesFlat, unsatisfiedShapesByRequiredVariable, untestedShapes);
    }

    @Override
    public int compareTo(SearchNode o) {
        int cmp = getPriority(this) - getPriority(o);
        if (cmp != 0) {
            return cmp;
        }
        return (int) (this.id - o.id);
    }
}
