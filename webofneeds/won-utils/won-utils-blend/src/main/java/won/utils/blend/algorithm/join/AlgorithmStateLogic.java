package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Node;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public abstract class AlgorithmStateLogic {
    /**
     * Returns all shapes for which the required variables are bound in the
     * specified bindings.
     *
     * @param state
     * @param bindings
     * @return
     */
    public static Set<ShapeFocusNode> getApplicableShapes(AlgorithmState state, VariableBindings bindings) {
        Set<Node> varsToFindShapesFor = new HashSet<>();
        varsToFindShapesFor.addAll(bindings.getBoundVariables());
        // leaving this line here to remind my future self that we actually do NOT want
        // to
        // add shapes to validate bound nodes that are variables - they are wildcards
        // that should not be validated.
        // varsToFindShapesFor.addAll(bindings.getBoundNodes());
        Set<ShapeFocusNode> ret = state.boundVarsToShapesToCheck.entrySet().stream()
                        .filter(e -> varsToFindShapesFor.containsAll(e.getKey()))
                        .map(Map.Entry::getValue)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
        return ret;
    }

    public static void recordBoundVarsToShapesToCheck(AlgorithmState state, SearchNode searchNode) {
        for (Set<Node> encounteredVariables : searchNode.encounteredVariables) {
            for (Node shape : searchNode.shapeToFocusNodes.keySet()) {
                for (Node focusNode : searchNode.shapeToFocusNodes.getOrDefault(shape, Collections.emptySet())) {
                    ShapeFocusNode shapeFocusNode = new ShapeFocusNode(shape, focusNode);
                    state.boundVarsToShapesToCheck.compute(encounteredVariables, (k, v) -> {
                        if (v == null) {
                            return new HashSet<>(Set.of(shapeFocusNode));
                        } else {
                            v.add(shapeFocusNode);
                            return v;
                        }
                    });
                }
            }
        }
    }

    public static boolean addToOpenNodes(AlgorithmState state, SearchNode searchNode) {
        state.log.debugFmt("considering node with bindings "
                        + (searchNode.bindings.isEmptyWhichMeansAllVariablesUndecided() ? "[]"
                                        : searchNode.bindings.toString()));
        if (searchNode.invalid) {
            state.log.debugFmt("Skipping invalid node. Violated shape: %s", searchNode.invalidShape);
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
        if (state.blendingInstance.blendingOptions.getUnboundHandlingMode().isUnboundAllowedIfNoOtherBinding()) {
            if (state.results.stream().anyMatch(n -> isAllVariablesBoundOrSubsetUnboundInNode(
                            searchNode.bindings.getUnboundNonBlankVariables(), n))) {
                state.log.debug(() -> "skipping node because the unboundHandlingMode only allows an unbound variable "
                                + "in the result if there is no result in which that variable is bound, "
                                + "and this node violates the condition");
                return false;
            }
        }
        SearchNode nodeToAdd = searchNode;
        /*
         * Optional<SearchNode> nodeWithSameBindings = state.openNodes.stream()
         * .filter(n -> n.id != searchNode.id && !
         * n.bindings.isEmptyWhichMeansAllVariablesUndecided() &&
         * n.bindings.equals(searchNode.bindings)).findFirst(); if
         * (nodeWithSameBindings.isPresent()){ state.log.debug(()
         * ->"found node with same bindings in open list, attempting to join...");
         * Optional<SearchNode> joinResult = SearchNodeLogic.join(searchNode,
         * nodeWithSameBindings.get()); if (joinResult.isPresent()) { nodeToAdd =
         * joinResult.get(); state.log.debug(() ->"nodes joined"); } else {
         * state.log.debug(() ->"nodes are not compatible"); } }
         */
        int sizeBeforeAdd = state.openNodes.size();
        state.openNodes.add(nodeToAdd);
        final SearchNode finalNodeToAdd = nodeToAdd;
        if (state.openNodes.size() > sizeBeforeAdd) {
            SearchNodeFormatter formatter = new SearchNodeFormatter(state);
            state.log.debug(() -> "adding search node:");
            state.log.logIndented(() -> state.log.debug(() -> formatter.format(finalNodeToAdd)));
        } else {
            SearchNodeFormatter formatter = new SearchNodeFormatter(state);
            state.log.debug(() -> "node already in open set, ignoring");
            state.log.logIndented(() -> state.log.debug(() -> formatter.format(finalNodeToAdd)));
            if (state.log.isDebugEnabled()) {
                state.log.debug(() -> "identical node(s) in open set:");
                state.openNodes.stream().filter(s -> s.equals(finalNodeToAdd))
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

    public static Set retainUnexplored(AlgorithmState state, Set<Node> variables) {
        return variables.stream().filter(var -> isUnexploredVariable(state, var)).collect(toSet());
    }

    public static boolean isUnexploredVariable(AlgorithmState state, Node candidate) {
        return state.unexploredVariables.contains(candidate);
    }

    public static boolean containsUnexploredVariable(AlgorithmState state, Collection<Node> candidates) {
        return candidates.stream().anyMatch(c -> isUnexploredVariable(state, c));
    }

    public static int countUnexploredVariables(AlgorithmState state, Collection<Node> candidates) {
        return (int) candidates.stream().filter(c -> isUnexploredVariable(state, c)).count();
    }

    public static Set retainExplored(AlgorithmState state, Set<Node> variables) {
        return variables.stream().filter(var -> isExploredVariable(state, var)).collect(toSet());
    }

    public static boolean isExploredVariable(AlgorithmState state, Node candidate) {
        return state.exploredVariables.contains(candidate);
    }

    public static boolean containsExploredVariable(AlgorithmState state, Collection<Node> candidates) {
        return candidates.stream().anyMatch(c -> isExploredVariable(state, c));
    }

    public static int countExploredVariables(AlgorithmState state, Collection<Node> candidates) {
        return (int) candidates.stream().filter(c -> isExploredVariable(state, c)).count();
    }

    public static boolean isBoundVariableInResult(AlgorithmState state, Set<Node> unbound) {
        return state.boundVariablesInResult.stream().anyMatch(varInResult -> unbound.contains(varInResult));
    }

    public static boolean isAllUnboundVariablesBoundOrSubsetUnboundInResult(AlgorithmState state, Set<Node> unbound) {
        return state.results.stream().anyMatch(n -> isAllVariablesBoundOrSubsetUnboundInNode(unbound, n));
    }

    private static boolean isAllVariablesBoundOrSubsetUnboundInNode(Set<Node> unbound, SearchNode n) {
        if (unbound.isEmpty()) {
            return false;
        }
        Set<Node> boundInNode = n.bindings.getBoundNonBlankVariables();
        if (boundInNode.containsAll(unbound)) {
            return true;
        }
        Set<Node> unboundInNode = n.bindings.getUnboundNonBlankVariables();
        if (unboundInNode.size() < unbound.size() && unbound.containsAll(unboundInNode)) {
            return true;
        }
        return false;
    }

    public static void addResult(AlgorithmState state, SearchNode currentNode) {
        // state.boundVariablesInResult.addAll(currentNode.bindings.getBoundVariables());
        // state.unboundVariablesInResult.addAll(currentNode.bindings.getUnboundVariables());
        state.results.add(currentNode);
        if (state.blendingInstance.blendingOptions.getUnboundHandlingMode().isUnboundAllowedIfNoOtherBinding()) {
            state.results.removeIf(
                            n -> isAllVariablesBoundOrSubsetUnboundInNode(n.bindings.getUnboundNonBlankVariables(),
                                            currentNode));
            state.openNodes.removeIf(n -> isAllVariablesBoundOrSubsetUnboundInNode(
                            n.bindings.getExplicitlyUnboundNonBlankVariables(), currentNode));
        }
        // if (state.unboundVariablesInResult.stream().anyMatch(var ->
        // state.boundVariablesInResult.contains(var))){
        // state.results.removeIf(n ->
        // n.bindings.getUnboundVariables().stream().anyMatch( unbound ->
        // state.boundVariablesInResult.contains(unbound)));
        // }
        // state.unboundVariablesInResult.clear();
        // state.results.forEach(n ->
        // state.unboundVariablesInResult.addAll(n.bindings.getUnboundVariables()));
    }
}
