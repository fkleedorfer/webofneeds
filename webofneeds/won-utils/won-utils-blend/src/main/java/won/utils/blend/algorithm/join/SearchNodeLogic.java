package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.graph.BlendedGraphs;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SearchNodeLogic {
    public static SearchNode forInitialShapeAndBindings(AlgorithmState state, Shape initialShape,
                    VariableBindings initialBindings, Set<Node> variables) {
        SearchNode newNode = new SearchNode(state, variables);
        newNode.untestedShapes.add(initialShape);
        newNode.bindings.setAll(initialBindings.getBindingsAsSet());
        return newNode;
    }

    public static Optional<SearchNode> join(SearchNode left, SearchNode right) {
        if (left.bindings.conflictsWith(right.bindings)) {
            return Optional.empty();
        }
        try {
            SearchNode sn = combine(left, right);
            return Optional.ofNullable(sn);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static SearchNode combine(SearchNode left, SearchNode right) {
        SearchNode combined = new SearchNode(left, right);
        combined.bindings.setAll(combined.bindings.mergeWith(left.bindings));
        combined.bindings.setAll(combined.bindings.mergeWith(right.bindings));
        combined.untestedShapes.addAll(left.untestedShapes);
        combined.untestedShapes.addAll(right.untestedShapes);
        left.encounteredVariables.stream().forEach(vars -> combined.encounteredVariables.add(new HashSet<>(vars)));
        right.encounteredVariables.stream().forEach(vars -> combined.encounteredVariables.add(new HashSet<>(vars)));
        combined.satisfiedShapes.addAll(left.satisfiedShapes);
        combined.satisfiedShapes.addAll(right.satisfiedShapes);
        mergeUnsatisfied(combined, right);
        mergeUnsatisfied(combined, left);
        combined.exploring.addAll(left.exploring);
        combined.exploring.addAll(right.exploring);
        return combined;
    }

    private static Set<Node> getEncounteredVariablesFlat(SearchNode node) {
        return node.encounteredVariables.stream().flatMap(v -> v.stream()).collect(Collectors.toUnmodifiableSet());
    }

    private static void mergeUnsatisfied(SearchNode combined, SearchNode other) {
        other.unsatisfiedShapesByRequiredVariable.entrySet().forEach(entry -> {
            combined.unsatisfiedShapesByRequiredVariable.compute(entry.getKey(), (key, values) -> {
                if (values == null) {
                    values = new HashSet<>();
                }
                values.addAll(entry.getValue());
                return values;
            });
        });
    }

    public static void recalculateDependentValues(SearchNode node, AlgorithmState state) {
        node.encounteredVariables.forEach(vars -> vars.removeAll(node.bindings.getDecidedVariables()));
        node.encounteredVariables.removeIf(vars -> vars.isEmpty());
        node.encounteredVariablesFlat.clear();
        node.encounteredVariablesFlat.addAll(getEncounteredVariablesFlat(node));
        node.exploring.removeAll(node.bindings.getDecidedVariables());
        node.exploring.removeAll(node.bindings.getBoundNodes());
        // node.blendedGraphSize = calculateBlendedGraphSize(state, node);
    }

    private static int calculateBlendedGraphSize(AlgorithmState state, SearchNode node) {
        BlendedGraphs blended = new BlendedGraphs(
                        state.blendingInstance.leftTemplate.getTemplateGraphs().getDataGraph(),
                        state.blendingInstance.rightTemplate.getTemplateGraphs().getDataGraph(), node.bindings, false);
        return blended.size();
    }

    public static int getPriority(SearchNode node) {
        if (node.bindings.isAllNonBlankVariablesBound()) {
            return 0;
        }
        int encounteredExplored = AlgorithmStateLogic.countExploredVariables(node.state, node.encounteredVariablesFlat);
        int encounteredUnexplored = AlgorithmStateLogic.countUnexploredVariables(node.state,
                        node.encounteredVariablesFlat);
        int numberOfVariables = node.state.allVariables.size();
        if (encounteredUnexplored > 0) {
            return encounteredUnexplored + numberOfVariables * encounteredExplored;
        } else if (encounteredExplored > 0) {
            return (int) Math.pow((double) numberOfVariables, (double) 2) * encounteredExplored;
            // } else if (node.blendedGraphSize > 0) {
            // return (int) Math.pow((double) numberOfVariables, (double) 3) *
            // node.blendedGraphSize;
        } else if (node.bindings.sizeNonBlankExcludingExplicitlyUnbound() > 0) {
            return (int) Math.pow((double) numberOfVariables, (double) 3)
                            * node.bindings.sizeExcludingExplicitlyUnbound();
        } else if (node.bindings.size() > 0) {
            return (int) Math.pow((double) numberOfVariables, (double) 4) * node.bindings.size();
        }
        return Integer.MAX_VALUE;
    }

    public static void addUnsatisfiedShapeByVariable(SearchNode searchNode, Shape shape, Node variable) {
        searchNode.unsatisfiedShapesByRequiredVariable.compute(variable, (key, values) -> {
            if (values == null) {
                values = new HashSet<>();
            }
            values.add(shape);
            return values;
        });
    }

    public static void removeUnsatisfiedShapeByVariable(SearchNode searchNode, Shape shape, Node variable) {
        searchNode.unsatisfiedShapesByRequiredVariable.compute(variable, (key, values) -> {
            if (values == null) {
                return null;
            }
            values.remove(shape);
            if (values.isEmpty()) {
                return null;
            }
            return values;
        });
    }
}
