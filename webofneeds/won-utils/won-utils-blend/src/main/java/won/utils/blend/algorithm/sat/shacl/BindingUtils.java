package won.utils.blend.algorithm.sat.shacl;

import org.apache.jena.graph.Node;
import won.utils.blend.BLEND;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public abstract class BindingUtils {
    private static final int MAX_REFERENCE_CHAIN_LENGTH = 2;

    public static boolean isBoundVariable(Node node, Map<Node, Node> bindings) {
        return isBoundVariable(node, n -> Optional.ofNullable(bindings.get(n)));
    }

    public static boolean isBoundVariable(Node node, Function<Node, Optional<Node>> dereferencer) {
        return dereferencer.apply(node).isPresent();
    }

    public static Node dereferenceIfVariable(Node node, Map<Node, Node> bindings) {
        return dereferenceIfVariable(node, n -> Optional.ofNullable(bindings.get(n)));
    }

    public static Node dereferenceIfVariable(Node node, Collection<VariableBinding> bindings) {
        return dereferenceIfVariable(node, n -> bindings.stream()
                        .filter(b -> b.getVariable().equals(n))
                        .map(VariableBinding::getBoundNode)
                        .findAny());
    }

    public static Node dereferenceIfVariable(Node node, Function<Node, Optional<Node>> dereferencer) {
        return dereferenceIfVariable(node, node, MAX_REFERENCE_CHAIN_LENGTH, MAX_REFERENCE_CHAIN_LENGTH, dereferencer);
    }

    private static Node dereferenceIfVariable(Node node, Node source, int remainingAllowedChainLength,
                    int totalAllowedChainLength, Function<Node, Optional<Node>> dereferencer) {
        Node dereferenced = dereferencer.apply(node).orElse(null);
        if (dereferenced == null || BLEND.unbound.equals(dereferenced)) {
            return node;
        }
        if (remainingAllowedChainLength == 0) {
            throw new IllegalArgumentException(
                            String.format("Dereferencing variable %s would take more than %d steps", source.toString(),
                                            totalAllowedChainLength));
        }
        return dereferenceIfVariable(dereferenced, source, remainingAllowedChainLength - 1, totalAllowedChainLength,
                        dereferencer);
    }

    public static boolean isValidReferenceChainLength(Node node, Collection<VariableBinding> bindings) {
        return isValidReferenceChainLength(node,
                        n -> bindings.stream().filter(b -> b.getVariable().equals(n)).map(VariableBinding::getBoundNode)
                                        .findAny());
    }

    public static boolean isValidReferenceChainLength(Node node, Map<Node, Node> bindings) {
        return isValidReferenceChainLength(node, n -> Optional.ofNullable(bindings.get(n)));
    }

    public static boolean isValidReferenceChainLength(Node node, Function<Node, Optional<Node>> dereferencer) {
        return isValidReferenceChainLength(node, dereferencer, MAX_REFERENCE_CHAIN_LENGTH);
    }

    private static boolean isValidReferenceChainLength(Node node, Function<Node, Optional<Node>> dereferencer,
                    int remainingAllowedLength) {
        Node dereferenced = dereferencer.apply(node).orElse(null);
        if (dereferenced == null || BLEND.unbound.equals(dereferenced)) {
            return true;
        }
        if (remainingAllowedLength > 0) {
            return isValidReferenceChainLength(dereferenced, dereferencer, remainingAllowedLength - 1);
        }
        return false;
    }

    private static boolean isValidReferenceChainLength(Node node, Collection<VariableBinding> bindings,
                    int remainingAllowedLength) {
        Node deref = dereference(node, bindings);
        if (deref == null || BLEND.unbound.equals(deref)) {
            return true;
        }
        if (remainingAllowedLength > 0) {
            return isValidReferenceChainLength(deref, bindings, remainingAllowedLength - 1);
        }
        return false;
    }

    private static Node dereferenceIfVariable(Node node, Node source, int remainingAllowedChainLength,
                    int totalAllowedChainLength, Collection<VariableBinding> bindings) {
        Node dereferenced = dereference(node, bindings);
        if (dereferenced == null || dereferenced.equals(BLEND.unbound)) {
            return node;
        }
        if (remainingAllowedChainLength == 0) {
            throw new IllegalArgumentException(
                            String.format("Dereferencing variable %s would take more than %d steps", source.toString(),
                                            totalAllowedChainLength));
        }
        return dereferenceIfVariable(dereferenced, source, remainingAllowedChainLength - 1, totalAllowedChainLength,
                        bindings);
    }

    private static Node dereference(Node node,
                    Collection<VariableBinding> bindings) {
        Node dereferenced = bindings.stream()
                        .filter(b -> b.getVariable().equals(node))
                        .map(VariableBinding::getBoundNode)
                        .findAny()
                        .orElse(null);
        return dereferenced;
    }

    public static Set<Node> dereferenceIfVariableToSet(Set<Node> nodes,
                    Collection<VariableBinding> bindings) {
        return nodes.stream()
                        .map(n -> dereferenceIfVariable(n, bindings))
                        .collect(Collectors.toSet());
    }

    public static List<Node> dereferenceIfVariableToList(List<Node> nodes,
                    Collection<VariableBinding> bindings) {
        return nodes.stream()
                        .map(n -> dereferenceIfVariable(n, bindings))
                        .collect(Collectors.toList());
    }

    public static BindingExtractionState applyToAllBindingOptions(
                    BindingExtractionState state,
                    Node node,
                    Function<BindingExtractionState, BindingExtractionState> fun, ExtractionContext context) {
        if (!state.isVariable(node)) {
            return dereferenceNodesAndApplyFunction(state, fun, null, context);
        }
        Node variable = node;
        if (state.alreadyDecidedVariable(variable)) {
            return dereferenceNodesAndApplyFunction(state, fun, "already decided: " + node, context);
        }
        List<BindingExtractionState> childStates = new ArrayList<>();
        Set<Node> bindingOptions = state.getBindingOptions(variable)
                        .stream()
                        .filter(n -> state.isBindingAllowed(variable, n))
                        .filter(n -> context.forbiddenBindings
                                        .stream().noneMatch(b -> b.getVariable().equals(variable)
                                                        && b.getBoundNode().equals(n)))
                        .collect(Collectors.toSet());
        context.indentedWriter.println(String.format("Variable %s could be bound to one of these:", variable));
        context.indentedWriter.incIndent();
        context.indentedWriter.println(String.format("%s\n ...trying all",
                        bindingOptions.stream().map(Object::toString).collect(joining("\n"))));
        context.indentedWriter.decIndent();
        for (Node newlyBoundNode : bindingOptions) {
            BindingExtractionState childState = dereferenceNodesAndApplyFunction(
                            state.addBinding(variable, newlyBoundNode), fun,
                            String.format("binding %s to %s", variable, newlyBoundNode), context);
            if (state.inheritedBindings.size() == 0
                            && childState.checkedBindings.size() == 1
                            && childState.containsInvalidBindings()) {
                VariableBinding forbidden = new VariableBinding(variable, newlyBoundNode);
                context.indentedWriter.println(String.format(
                                "singleton binding was found invalid, will not be tried again: %s", forbidden));
                context.forbiddenBindings.add(forbidden);
            }
            childStates.add(childState);
        }
        context.indentedWriter.println("all checked bindings:");
        context.indentedWriter.incIndent();
        childStates.stream().flatMap(s -> s.checkedBindings.stream()).forEach(cb -> context.indentedWriter.println(cb));
        context.indentedWriter.decIndent();
        return state.addCheckedBindings(childStates);
    }

    private static BindingExtractionState dereferenceNodesAndApplyFunction(BindingExtractionState state,
                    Function<BindingExtractionState, BindingExtractionState> fun, String message,
                    ExtractionContext context) {
        Set<Node> valueNodes = state.valueNodes;
        if (message != null) {
            context.indentedWriter.println(message);
        }
        state = state.setNodeAndResetValueNodes(dereferenceIfVariable(state.node, state.inheritedBindings));
        state = state.setValueNodes(dereferenceIfVariableToSet(valueNodes, state.inheritedBindings));
        context.indentedWriter.incIndent();
        BindingExtractionState ret = fun.apply(state);
        context.indentedWriter.decIndent();
        return ret;
    }

    public static BindingExtractionState applyToAllBindingOptions(
                    BindingExtractionState state,
                    Set<Node> nodes,
                    Function<BindingExtractionState, BindingExtractionState> fun, ExtractionContext context) {
        if (nodes.isEmpty()) {
            return dereferenceNodesAndApplyFunction(state, fun, null, context);
        }
        Node current = nodes.stream().findAny().get();
        Set<Node> remaining = nodes.stream().filter(n -> !n.equals(current)).collect(Collectors.toSet());
        return applyToAllBindingOptions(state, current, s -> applyToAllBindingOptions(s, remaining, fun, context),
                        context);
    }

    public static Set<Node> getVariablesBoundToNode(Node node, Function<Node, Set<Node>> inverseDereferencer) {
        Set<Node> vars = inverseDereferencer.apply(node);
        vars.addAll(vars.stream().flatMap(v -> inverseDereferencer.apply(v).stream()).collect(Collectors.toSet()));
        return vars;
    }

    public static Set<Node> getVariablesBoundToNode(Node node, Map<Node, Node> bindings) {
        return getVariablesBoundToNode(node, n -> bindings.entrySet().stream().filter(e -> n.equals(e.getValue())).map(
                        Map.Entry::getKey).collect(Collectors.toSet()));
    }
}
