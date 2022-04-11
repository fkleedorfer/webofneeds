package won.utils.blend.support.bindings;

import org.apache.jena.graph.Node;
import won.utils.blend.BLEND;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class VariableBindings {
    private static final int MAX_REFERENCE_CHAIN_LENGTH = 2;
    private Map<Node, Node> bindings;
    private Set<Node> variables;
    private Set<Node> blankNodeVariables;
    private Set<Node> nonBlankVariables;

    public VariableBindings(VariableBindings toCopy) {
        this(toCopy.variables, toCopy.bindings);
    }

    public VariableBindings(Set<Node> variables) {
        this.bindings = new HashMap<>();
        this.variables = Collections.unmodifiableSet(new HashSet<>(variables));
        this.blankNodeVariables = this.variables.stream().filter(Node::isBlank).collect(Collectors.toUnmodifiableSet());
        this.nonBlankVariables = this.variables.stream().filter(not(Node::isBlank))
                        .collect(Collectors.toUnmodifiableSet());
    }

    public VariableBindings(Set<Node> variables, Set<VariableBinding> fromBindings) {
        this.variables = new HashSet<>(variables);
        this.blankNodeVariables = this.variables.stream().filter(Node::isBlank).collect(Collectors.toUnmodifiableSet());
        this.nonBlankVariables = this.variables.stream().filter(not(Node::isBlank))
                        .collect(Collectors.toUnmodifiableSet());
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
                            "One or more variables are unknown in specified bindings: " + bindings.keySet()
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
        this.blankNodeVariables = this.variables.stream().filter(Node::isBlank).collect(Collectors.toUnmodifiableSet());
        this.nonBlankVariables = this.variables.stream().filter(not(Node::isBlank))
                        .collect(Collectors.toUnmodifiableSet());
        this.bindings = new HashMap<>(bindings);
        if (isInvariantViolated()) {
            throw new IllegalArgumentException("invariant violated by bindings " + bindings);
        }
    }

    /**
     * Returns the bindings as a Set of {@link VariableBinding}, including bindings
     * to bl:unbound.
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
     * Adds the new binding if it is possible (if the node is a yet undecided
     * variable and adding the binding does not violate the invariant.
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
                        // all keys in bindings map must be in variables set
                        !variables.containsAll(bindings.keySet())
                                        // no variable may be bound to itself
                                        || bindings.entrySet().stream().anyMatch(e -> e.getKey().equals(e.getValue()))
                                        // no reference chains longer than 2
                                        || !bindings.keySet().stream().allMatch(this::isValidReferenceChainLength)
                                        // no blank node variables bound to non-blank variables (except blank ->
                                        // bl:unbound)
                                        || bindings.entrySet().stream()
                                        .anyMatch(e -> e.getKey().isBlank() != e.getValue().isBlank()
                                                        && !e.getValue().equals(BLEND.unbound))
                                        // blank node vars cannot be bound to explicitly unbound blank node vars.
                                        // the same configuration is ok for non-blank vars because it reduces
                                        // the degrees of freedom. for blank nodes, though, unbound means the blanks
                                        // are rendered as-is so this configuration raises the degrees of freedom
                                        // e.g.: b1 -> b2 -> blank; b2 -> b1 -> blank; b1 -> blank, b2 -> blank
                                        // is three equivalent solutions instead of one
                                        || bindings.entrySet().stream()
                                        .anyMatch(e -> e.getKey().isBlank()
                                                        && e.getValue().isBlank()
                                                        && dereferenceIfVariable(e.getKey()).isEmpty());
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
     * Returns the number of non-blank node variables bound to anything but
     * bl:unbound.
     *
     * @return
     */
    public int sizeNonBlankExcludingExplicitlyUnbound() {
        return (int) bindings.entrySet().stream()
                        .filter(e -> !(e.getKey().isBlank() || e.getValue().equals(BLEND.unbound))).count();
    }

    /**
     * Returns all variables that are bound to anything but bl:unbound.
     *
     * @return
     */
    public Set<Node> getBoundVariables() {
        return bindings.entrySet().stream()
                        .filter(e -> !e.getValue().equals(BLEND.unbound))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
    }

    public Set<Node> getBoundNonBlankVariables() {
        return bindings.entrySet().stream()
                        .filter(e -> !(e.getKey().isBlank() || e.getValue().equals(BLEND.unbound)))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
    }

    public boolean isVariable(Node variable) {
        return variables.contains(variable);
    }

    public boolean isNonBlankVariable(Node variable) {
        return nonBlankVariables.contains(variable);
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
     * Returns an Optional of the {@link Node} the specified <code>variable</code>
     * is bound to. If the specified <code>variable</code> is not a variable or not
     * bound, or bound to <code>bl:unbound</code>, the optional is empty.
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

    /**
     * Applies {@link #dereferenceIfVariable(Node)} to each element, returning the result if present, otherwise the
     * provided node, collected as a List.
     *
     * @param variables
     * @return
     */
    public List<Node> dereferenceIfVariableToList(List<Node> variables) {
        return variables.stream()
                        .map(n -> dereferenceIfVariable(n).orElse(n))
                        .collect(Collectors.toList());
    }

    /**
     * Applies {@link #dereferenceIfVariable(Node)} to each element, returning the result if present, otherwise the
     * * provided node, collected as a Set.
     *
     * @param variables
     * @return
     */
    public Set<Node> dereferenceIfVariableToSet(Set<Node> variables) {
        return variables.stream()
                        .map(n -> dereferenceIfVariable(n).orElse(n))
                        .collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VariableBindings that = (VariableBindings) o;
        return bindings.equals(that.bindings) && variables.equals(that.variables);
    }

    @Override
    public int hashCode() {
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
        return conflictsWith(other, false);
    }

    public boolean conflictsWithAllowOverrideUnbound(VariableBindings other){
        return conflictsWith(other, true);
    }

    public boolean conflictsWith(VariableBindings other, boolean allowOverrideUnbound) {
        for (Node var : variables) {
            Node ourValue = bindings.get(var);
            Node theirValue = other.bindings.get(var);
            if (ourValue != null && theirValue != null && !ourValue.equals(theirValue)) {
                if (allowOverrideUnbound && (theirValue.equals(BLEND.unbound) || ourValue.equals(BLEND.unbound))){
                    continue;
                }
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
        for (Map.Entry<Node, Node> entry: bindings.bindings.entrySet()){
            mergedBindings.merge(entry.getKey(), entry.getValue(), (l, r ) -> {
                if (l.equals(BLEND.unbound)){
                    return r;
                }
                if (r.equals(BLEND.unbound)) {
                    return l;
                }
                if (l.equals(r)){
                    return r;
                }
                throw new IllegalArgumentException("cannot merge, conflict for bindings of " + entry.getKey() + ": bound to " + l + " and " + r );
            } );
        }
        return new VariableBindings(variables, mergedBindings);
    }

    @Override
    public String toString() {
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
     * Returns true if there are no more variables or blank nodes left to bind (but
     * some may be bound to bl:unbound).
     *
     * @return
     */
    public boolean isAllVariablesDecided() {
        return bindings.keySet().containsAll(this.variables);
    }

    /**
     * Returns true iff all variables (non-blank) are bound to anything but
     * bl:unbound.
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

    public boolean isAllNonBlankVariablesBound() {
        return bindings.entrySet().stream()
                        .filter(e -> !e.getValue().equals(BLEND.unbound))
                        .filter(e -> !e.getValue().isBlank())
                        .map(e -> e.getKey())
                        .collect(Collectors.toSet())
                        .containsAll(this.nonBlankVariables);
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
     * Returns true if the variable is not bound or bound to bl:unbound.
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
     * Returns true if the variable is not a blank node and not bound or bound to
     * bl:unbound.
     *
     * @param variable
     * @return
     */
    public boolean isBoundNonBlankVariable(Node variable) {
        if (variable.isBlank()) {
            return false;
        }
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

    public Set<Node> getUnboundNonBlankVariables() {
        return nonBlankVariables.stream()
                        .filter(v -> bindings.getOrDefault(v, BLEND.unbound).equals(BLEND.unbound))
                        .collect(Collectors.toSet());
    }

    public Set<Node> getExplicitlyUnboundNonBlankVariables() {
        return bindings.entrySet().stream()
                        .filter(e -> e.getValue().equals(BLEND.unbound))
                        .map(Map.Entry::getKey)
                        .filter(not(Node::isBlank))
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

    public Set<Node> getVariablesBoundToNode(Node node) {
        return variables.stream()
                        .filter(var -> dereferenceIfVariable(var).map(boundNode -> node.equals(boundNode))
                                        .orElse(false))
                        .collect(Collectors.toUnmodifiableSet());
    }

    public void setUndecidedToExplicitlyUnbound() {
        variables.stream().forEach(var -> {
            if (!bindings.containsKey(var)) {
                bindings.put(var, BLEND.unbound);
            }
        });
    }
}
