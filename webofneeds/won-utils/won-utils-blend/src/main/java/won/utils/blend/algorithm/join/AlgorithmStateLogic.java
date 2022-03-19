package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public abstract class AlgorithmStateLogic {

    /**
     * Returns all shapes for which the required variables are bound in the specified bindings.
     *
     * @param state
     * @param bindings
     * @return
     */
    public static Set<Shape> getApplicableShapes(AlgorithmState state, VariableBindings bindings) {
        Set<Node> varsToFindShapesFor = new HashSet<>();
        varsToFindShapesFor.addAll(bindings.getBoundVariables());
        // leaving this line here to remind my future self that we actually do NOT want to
        // add shapes to validate bound nodes that are variables - they are wildcards that should not be validated.
        // varsToFindShapesFor.addAll(bindings.getBoundNodes());

        Set<Shape> ret = state.requiredVariablesByShapes.entrySet()
                        .stream()
                        .filter(e -> e.getValue().stream().anyMatch(vars -> varsToFindShapesFor.containsAll(vars)))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
        return ret;
    }

    public static void recordRequiredVariablesForShape(AlgorithmState state, Shape shape,
                    Set<Set<Node>> encounteredVariables) {
        state.requiredVariablesByShapes.put(shape, Collections.unmodifiableSet(encounteredVariables));
    }

    public static boolean addToOpenNodes(AlgorithmState state, SearchNode searchNode) {
        if (searchNode.invalid) {
            if (state.log.isFinerTraceEnabled()){
                SearchNodeLogic.recalculateDependentValues(searchNode, state);
                state.log.finerTrace(() -> "skipping invalid node:");
                state.log.logIndented(() -> state.log.finerTrace(() -> SearchNodeFormatter.format(state, searchNode)));
            }
            return false;
        }
        SearchNodeLogic.recalculateDependentValues(searchNode, state);
        if (state.closedNodes.contains(searchNode)) {
            if (state.log.isDebugEnabled()) {
                state.log.debug(() -> "node equals a closed node, ignoring");
                state.log.logIndented(() -> state.log.trace(
                                () -> "ignored node:\n" + SearchNodeFormatter.format(state, searchNode)));
            }
            return false;
        }
        int sizeBeforeAdd = state.openNodes.size();
        state.openNodes.add(searchNode);
        if (state.openNodes.size() > sizeBeforeAdd) {
            SearchNodeFormatter formatter = new SearchNodeFormatter(state);
            state.log.debug(() -> "adding search node:");
            state.log.logIndented(() -> state.log.debug(() -> formatter.format(searchNode)));
        } else {
            SearchNodeFormatter formatter = new SearchNodeFormatter(state);
            state.log.debug(() -> "node already in open set, ignoring");
            state.log.logIndented(() -> state.log.debug(() -> formatter.format(searchNode)));
            if (state.log.isDebugEnabled()){
                state.log.debug(() -> "identical node(s) in open set:");
                state.openNodes
                                .stream()
                                .filter(s -> s.equals(searchNode))
                                .forEach(s -> state.log.logIndented(() -> state.log.debug(() -> formatter.format(s))));
            }
            return false;
        }
        return true;
    }

    public static void addSearchNodes(AlgorithmState state, Set<SearchNode> initialNodes) {
        initialNodes.forEach(node -> addToOpenNodes(state, node));
    }

    public static void removeSearchNode(AlgorithmState state, SearchNode toRemove) {
        state.openNodes.remove(toRemove);
    }

    public static void removeSearchNodes(AlgorithmState state, Set<SearchNode> toRemove) {
        toRemove.forEach(node -> removeSearchNode(state, node));
    }

    public static Set retainUnexplored(AlgorithmState state, Set<Node> variables){
        return variables.stream().filter(var -> isUnexploredVariable(state, var)).collect(toSet());
    }

    public static boolean isUnexploredVariable(AlgorithmState state, Node candidate) {
        return state.unexploredVariables.contains(candidate);
    }

    public static boolean containsUnexploredVariable(AlgorithmState state, Collection<Node> candidates) {
        return candidates.stream().anyMatch(c -> isUnexploredVariable(state, c));
    }

    public static int countUnexploredVariables(AlgorithmState state, Collection<Node> candidates){
        return (int) candidates.stream().filter(c -> isUnexploredVariable(state, c)).count();
    }

    public static Set retainExplored(AlgorithmState state, Set<Node> variables){
        return variables.stream().filter(var -> isExploredVariable(state, var)).collect(toSet());
    }

    public static boolean isExploredVariable(AlgorithmState state, Node candidate) {
        return state.exploredVariables.contains(candidate);
    }

    public static boolean containsExploredVariable(AlgorithmState state, Collection<Node> candidates) {
        return candidates.stream().anyMatch(c -> isExploredVariable(state, c));
    }

    public static int countExploredVariables(AlgorithmState state, Collection<Node> candidates){
        return (int) candidates.stream().filter(c -> isExploredVariable(state, c)).count();
    }

    public static boolean isBoundVariableInResult(AlgorithmState state, Set<Node> unbound) {
        return state.boundVariablesInResult.stream().anyMatch(varInResult -> unbound.contains(varInResult));
    }

    public static boolean isAllUnboundVariablesBoundInResult(AlgorithmState state, Set<Node> unbound) {
        return state.results.stream().anyMatch(n -> isAllVariablesBoundInNode(unbound, n));
    }

    private static boolean isAllVariablesBoundInNode(Set<Node> unbound, SearchNode n) {
        if (unbound.isEmpty()){
            return false;
        }
        return n.bindings.getBoundVariables().containsAll(unbound);
    }

    public static void addResult(AlgorithmState state,
                    SearchNode currentNode) {
        //state.boundVariablesInResult.addAll(currentNode.bindings.getBoundVariables());
        //state.unboundVariablesInResult.addAll(currentNode.bindings.getUnboundVariables());
        state.results.add(currentNode);
        if (state.blendingInstance.blendingOptions.getUnboundHandlingMode().isUnboundAllowedIfNoOtherBinding()) {
            state.results.removeIf(n -> isAllVariablesBoundInNode(n.bindings.getUnboundVariables(), currentNode));
            state.openNodes.removeIf(n -> isAllVariablesBoundInNode(n.bindings.getExplicitlyUnboundVariables(), currentNode));
        }
        //if (state.unboundVariablesInResult.stream().anyMatch(var -> state.boundVariablesInResult.contains(var))){
        //    state.results.removeIf(n -> n.bindings.getUnboundVariables().stream().anyMatch( unbound -> state.boundVariablesInResult.contains(unbound)));
        //}
        //state.unboundVariablesInResult.clear();
        //state.results.forEach(n -> state.unboundVariablesInResult.addAll(n.bindings.getUnboundVariables()));

    }
}
