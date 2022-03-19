package won.utils.blend.support.bindings;

import org.apache.jena.graph.Node;
import won.utils.blend.BLEND;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class VariableBindings {
    private static final int MAX_REFERENCE_CHAIN_LENGTH = 2;
    private Map<Node, Node> bindings;
    private Set<Node> variables;

    public VariableBindings(VariableBindings toCopy) {
        this(toCopy.variables, toCopy.bindings);
    }

    public VariableBindings(Set<Node> variables) {
        this.bindings = new HashMap<>();
        this.variables = new HashSet<>(variables);
    }

    public VariableBindings(Set<Node> variables, Set<VariableBinding> fromBindings) {
        this.variables = new HashSet<>(variables);
        setAll(fromBindings);
        if (isInvariantViolated()) {
            throw new IllegalArgumentException("invariant violated by bindings " + bindings);
        }
    }

    public void setAll(Set<VariableBinding> fromBindings) {
        this.bindings = fromBindings.stream()
                        .collect(
                                        toMap(
                                                        VariableBinding::getVariable,
                                                        VariableBinding::getBoundNode));
        if (!variables.containsAll(bindings.keySet())) {
            throw new IllegalArgumentException(
                            "One or more variables are unknown among in specified bindings: " + bindings.keySet()
                                            .stream().filter(v -> !variables.contains(v)).map(Object::toString)
                                            .collect(joining(" ,")));
        }
        if (isInvariantViolated()) {
            throw new IllegalArgumentException("invariant violated by bindings " + bindings);
        }
    }

    public void setAll(VariableBindings bindings) {
        this.setAll(bindings.getBindingsAsSet());
        if (isInvariantViolated()) {
            throw new IllegalArgumentException("invariant violated by bindings " + bindings);
        }
    }

    public VariableBindings(Set<Node> variables, Map<Node, Node> bindings) {
        this.variables = new HashSet<>(variables);
        this.bindings = new HashMap<>(bindings);
        if (isInvariantViolated()) {
            throw new IllegalArgumentException("invariant violated by bindings " + bindings);
        }
    }

    /**
     * Returns the bindings as a Set of {@link VariableBinding}, including bindings to bl:unbound.
     *
     * @return
     */
    public Set<VariableBinding> getBindingsAsSet() {
        return bindings.entrySet().stream().map(e -> new VariableBinding(e.getKey(), e.getValue()))
                        .collect(Collectors.toSet());
    }

    /**
     * Returns all variables that have been decided (but possibly set to bl:unbound.
     *
     * @return
     */
    public Set<Node> getDecidedVariables() {
        return Collections.unmodifiableSet(bindings.keySet());
    }

    /**
     * Adds the new binding if it is possible (if the node is a yet undecided variable and adding the binding
     * does not violate the invariant.
     *
     * @param node
     * @param newlyBoundNode
     * @return
     */
    public boolean addBindingIfPossible(Node node, Node newlyBoundNode) {
        if (!isVariable(node)) {
            return false;
        }
        if (bindings.containsKey(node)) {
            return false;
        }
        this.bindings.put(node, newlyBoundNode);
        if (isInvariantViolated()) {
            this.bindings.remove(node);
            return false;
        }
        return true;
    }

    /**
     * Returns true if the invariant is violated.
     *
     * @return
     */
    private boolean isInvariantViolated() {
        return
                        !variables.containsAll(bindings.keySet()) ||
                                        bindings.entrySet().stream().anyMatch(e -> e.getKey().equals(e.getValue())) ||
                                        !bindings.keySet().stream().allMatch(this::isValidReferenceChainLength);
    }

    public int size() {
        return bindings.size();
    }

    /**
     * Returns the number of variables bound to anything but bl:unbound.
     *
     * @return
     */
    public int sizeExcludingExplicitlyUnbound() {
        return (int) bindings.entrySet().stream().filter(e -> !e.getValue().equals(BLEND.unbound)).count();
    }

    /**
     * Returns all variables that are bound to anything but bl:unbound.
     *
     * @return
     */
    public Set<Node> getBoundVariables() {
        return bindings.entrySet().stream().filter(e -> !e.getValue().equals(BLEND.unbound)).map(Map.Entry::getKey)
                        .collect(
                                        Collectors.toSet());
    }

    public boolean isVariable(Node variable) {
        return variables.contains(variable);
    }

    /**
     * Returns true iff the variable has been set (possibly to bl:unbound).
     *
     * @param variable
     * @return
     */
    public boolean isAlreadyDecidedVariable(Node variable) {
        return bindings.containsKey(variable);
    }

    public boolean isUnboundOrUnsetVariable(Node variable) {
        return dereferenceIfVariable(variable).isEmpty();
    }

    /**
     * Returns an Optional of the {@link Node} the specified <code>variable</code> is bound to. If the specified
     * <code>variable</code> is not a variable or not bound, or bound to <code>bl:unbound</code>, the optional is empty.
     *
     * @param variable
     */
    public Optional<Node> dereferenceIfVariable(Node variable) {
        return dereferenceIfVariable(variable, n -> Optional.ofNullable(bindings.get(n)));
    }

    private Optional<Node> dereferenceIfVariable(Node variable, Function<Node, Optional<Node>> dereferencer) {
        return dereferenceIfVariable(variable, variable, MAX_REFERENCE_CHAIN_LENGTH, MAX_REFERENCE_CHAIN_LENGTH,
                        dereferencer);
    }

    private Optional<Node> dereferenceIfVariable(Node variable, Node source, int remainingAllowedChainLength,
                    int totalAllowedChainLength, Function<Node, Optional<Node>> dereferencer) {
        Node dereferenced = dereferencer.apply(variable).orElse(null);
        if (dereferenced == null || BLEND.unbound.equals(dereferenced)) {
            return Optional.empty();
        }
        if (isVariable(dereferenced)) {
            if (remainingAllowedChainLength == 0) {
                throw new IllegalArgumentException(
                                String.format("Dereferencing variable %s would take more than %d steps",
                                                source.toString(),
                                                totalAllowedChainLength));
            }
            return dereferenceIfVariable(dereferenced, source, remainingAllowedChainLength - 1, totalAllowedChainLength,
                            dereferencer);
        }
        return Optional.of(dereferenced);
    }

    public boolean isValidReferenceChainLength(Node variable) {
        return isValidReferenceChainLength(variable, n -> Optional.ofNullable(bindings.get(n)));
    }

    private boolean isValidReferenceChainLength(Node variable, Function<Node, Optional<Node>> dereferencer) {
        return isValidReferenceChainLength(variable, dereferencer, MAX_REFERENCE_CHAIN_LENGTH);
    }

    private boolean isValidReferenceChainLength(Node variable, Function<Node, Optional<Node>> dereferencer,
                    int remainingAllowedLength) {
        Node dereferenced = dereferencer.apply(variable).orElse(null);
        if (dereferenced == null || BLEND.unbound.equals(dereferenced)) {
            return true;
        }
        if (remainingAllowedLength > 0) {
            return isValidReferenceChainLength(dereferenced, dereferencer, remainingAllowedLength - 1);
        }
        return false;
    }

    public List<Node> dereferenceIfVariableToList(List<Node> variables) {
        return variables.stream()
                        .map(n -> dereferenceIfVariable(n))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
    }

    public List<Node> dereferenceIfVariableToSet(Set<Node> variables) {
        return variables.stream()
                        .map(n -> dereferenceIfVariable(n))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VariableBindings that = (VariableBindings) o;
        return bindings.equals(that.bindings) && variables.equals(that.variables);
    }

    @Override public int hashCode() {
        return Objects.hash(bindings, variables);
    }

    public boolean overlapsWith(VariableBindings other) {
        boolean found = false;
        boolean notFound = false;
        Set<Map.Entry<Node, Node>> otherEntries = other.bindings.entrySet();
        for (Map.Entry<Node, Node> entry : bindings.entrySet()) {
            if (otherEntries.contains(entry)) {
                found = true;
            } else {
                notFound = true;
            }
            if (found && notFound) {
                return true;
            }
        }
        return false;
    }

    public boolean conflictsWith(VariableBindings other) {
        for (Node var : variables) {
            Node ourValue = bindings.get(var);
            Node theirValue = other.bindings.get(var);
            if (ourValue != null && theirValue != null && !ourValue.equals(theirValue)) {
                return true;
            }
        }
        return false;
    }

    public VariableBindings mergeWith(VariableBindings bindings) {
        if (!this.variables.equals(bindings.variables)) {
            throw new IllegalArgumentException("Cannot merge bindings: underlying variables differ");
        }
        Map<Node, Node> mergedBindings = new HashMap<>(this.bindings);
        mergedBindings.putAll(bindings.bindings);
        return new VariableBindings(variables, mergedBindings);
    }

    @Override public String toString() {
        return bindings.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue())
                        .collect(Collectors.joining(", "));
    }

    public Set<Node> getVariables() {
        return new HashSet<>(variables);
    }

    public boolean isEmptyWhichMeansAllVariablesUndecided() {
        return bindings.isEmpty();
    }

    public Set<Node> getBoundNodes() {
        return bindings.values().stream().filter(v -> !BLEND.unbound.equals(v)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns true if there are no more variables left to bind (but some may be bound to bl:unbound).
     *
     * @return
     */
    public boolean isAllVariablesDecided() {
        return bindings.keySet().containsAll(this.variables);
    }

    /**
     * Returns true iff all variables are bound to anything but bl:unbound.
     *
     * @return
     */
    public boolean isAllVariablesBound() {
        return bindings.entrySet().stream()
                        .filter(e -> !e.getValue().equals(BLEND.unbound))
                        .map(e -> e.getKey())
                        .collect(Collectors.toSet())
                        .containsAll(this.variables);
    }

    /**
     * Returns true iff the variable is bound to bl:unbound.
     *
     * @param variable
     * @return
     */
    public boolean isExplicitlyUnbound(Node variable) {
        Node boundNode = bindings.get(variable);
        if (boundNode == null) {
            return false;
        }
        return BLEND.unbound.equals(boundNode);
    }

    /**
     * Returns true if the variable is not bound and not bound to bl:unbound.
     *
     * @param variable
     * @return
     */
    public boolean isBoundVariable(Node variable) {
        Node boundNode = bindings.get(variable);
        if (boundNode == null) {
            return false;
        }
        return !BLEND.unbound.equals(boundNode);
    }

    /**
     * Get all variables not bound or bound to bl:unbound.
     *
     * @return
     */
    public Set<Node> getUnboundVariables() {
        return variables.stream()
                        .filter(v -> bindings.getOrDefault(v, BLEND.unbound).equals(BLEND.unbound))
                        .collect(Collectors.toSet());
    }

    /**
     * Get all variables bound to bl:unbound.
     *
     * @return
     */
    public Set<Node> getExplicitlyUnboundVariables() {
        return bindings.entrySet().stream()
                        .filter(e -> e.getValue().equals(BLEND.unbound))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
    }

    /**
     * Returns the node the variable is bound to, unless it is bl:unbound.
     *
     * @param variable
     * @return
     */
    public Optional<Node> getBoundNode(Node variable) {
        Node boundNode = bindings.get(variable);
        return Optional.ofNullable(BLEND.unbound.equals(boundNode) ? null : boundNode);
    }

    public Set<Node> getVariablesBoundToVariables() {
        return bindings.entrySet()
                        .stream()
                        .filter(e -> isVariable(e.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toUnmodifiableSet());
    }

    public Set<Node> getVariablesBoundToConstants() {
        return bindings.entrySet()
                        .stream()
                        .filter(e -> !isVariable(e.getValue()) && !BLEND.unbound.equals(e.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toUnmodifiableSet());
    }
}
