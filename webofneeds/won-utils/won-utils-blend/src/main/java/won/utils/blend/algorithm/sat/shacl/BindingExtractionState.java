package won.utils.blend.algorithm.sat.shacl;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.sparql.graph.GraphFactory;
import won.utils.blend.BLEND;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.graph.VariableAwareGraph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static won.utils.blend.algorithm.sat.shacl.BindingUtils.dereferenceIfVariable;
import static won.utils.blend.algorithm.sat.shacl.BindingUtils.isValidReferenceChainLength;
import static won.utils.blend.algorithm.sat.support.Ternary.*;

public class BindingExtractionState {
    public final List<VariableBinding> inheritedBindings;
    public final Set<CheckedBindings> checkedBindings;
    public final Node node;
    public final Set<Node> valueNodes;
    public final BlendingInstance instance;

    private BindingExtractionState(List<VariableBinding> inheritedBindings,
                    Set<CheckedBindings> checkedBindings, Node node, Set<Node> valueNodes,
                    BlendingInstance instance) {
        this.inheritedBindings = inheritedBindings
                        .stream()
                        .collect(Collectors.toUnmodifiableList());
        this.checkedBindings = checkedBindings
                        .stream()
                        .collect(Collectors.toUnmodifiableSet());
        this.node = node;
        this.valueNodes = Collections.unmodifiableSet(valueNodes);
        this.instance = instance;
    }

    public BindingExtractionState(
                    BlendingInstance instance) {
        this(
                        Collections.emptyList(),
                        Collections.emptySet(),
                        null, Collections.emptySet(),
                        instance);
    }

    public BindingExtractionState setNodeAndResetValueNodes(Node node) {
        if (node.equals(this.node) && this.valueNodes.isEmpty()) {
            return this;
        }
        return new BindingExtractionState(
                        this.inheritedBindings,
                        this.checkedBindings,
                        node,
                        Collections.emptySet(),
                        instance);
    }

    public BindingExtractionState setValueNodes(Set<Node> valueNodes) {
        return new BindingExtractionState(
                        this.inheritedBindings,
                        this.checkedBindings,
                        this.node,
                        valueNodes,
                        instance);
    }

    public BindingExtractionState addBinding(Node variable, Node boundNode) {
        List bindings = inheritedBindings.stream().collect(Collectors.toList());
        bindings.add(new VariableBinding(
                        variable,
                        boundNode,
                        instance.leftTemplate.isVariable(boundNode)
                                        || instance.rightTemplate.isVariable(boundNode)));
        return new BindingExtractionState(bindings, checkedBindings, node, valueNodes, instance);
    }

    public BindingExtractionState addCheckedBindings(BindingExtractionState state) {
        return addCheckedBindings(state.checkedBindings);
    }

    public BindingExtractionState addCheckedBindings(Set<CheckedBindings> checkedBindings) {
        Set<CheckedBindings> newCB = this.checkedBindings
                        .stream()
                        .collect(toSet());
        newCB.addAll(checkedBindings);
        return new BindingExtractionState(inheritedBindings, newCB, this.node, this.valueNodes,
                        instance);
    }

    public BindingExtractionState addCheckedBindings(Collection<BindingExtractionState> fromStates) {
        Set<CheckedBindings> newCB = this.checkedBindings
                        .stream()
                        .collect(toSet());
        newCB.addAll(fromStates.stream().flatMap(s -> s.checkedBindings.stream()).collect(toSet()));
        return new BindingExtractionState(inheritedBindings, newCB, this.node, this.valueNodes,
                        instance);
    }

    public BindingExtractionState addCheckedBindings(CheckedBindings checkedBindings) {
        Set<CheckedBindings> newCB = this.checkedBindings
                        .stream()
                        .collect(toSet());
        newCB.add(checkedBindings);
        return new BindingExtractionState(inheritedBindings, newCB, this.node, this.valueNodes,
                        instance);
    }

    public boolean isVariable(Node candidate) {
        return instance.leftTemplate.isVariable(candidate) || instance.rightTemplate
                        .isVariable(candidate);
    }

    public Set<Node> getBindingOptions(Node variable) {
        if (this.instance.leftTemplate.isVariable(variable)) {
            return Stream.concat(
                            Stream.concat(
                                            Stream.of(BLEND.unbound),
                                            instance.rightTemplate.getConstants().stream()),
                            instance.rightTemplate.getVariables().stream())
                            .collect(toSet());
        } else if (instance.rightTemplate.isVariable(variable)) {
            return Stream.concat(
                            Stream.concat(
                                            Stream.of(BLEND.unbound),
                                            instance.leftTemplate.getConstants().stream()),
                            instance.leftTemplate.getVariables().stream())
                            .collect(toSet());
        }
        return Collections.emptySet();
    }

    public Graph getBlendedDataWithoutBackground() {
        Graph currentBindings = GraphFactory.createDefaultGraph();
        for (VariableBinding binding : inheritedBindings) {
            Node boundNode = binding.getBoundNode();
            if (BLEND.unbound.equals(boundNode)) {
                continue;
            }
            Node variable = binding.getVariable();
            currentBindings.add(new Triple(variable, BLEND.boundTo, boundNode));
        }
        return new Union(
                        new Union(
                                        currentBindings,
                                        new Union(
                                                        instance.leftTemplate.getTemplateGraphs().getBindingsGraph(),
                                                        instance.rightTemplate.getTemplateGraphs().getBindingsGraph())),
                        getBlendedGraphs());
    }

    public Graph getBlendedData() {
        Graph currentBindings = GraphFactory.createDefaultGraph();
        for (VariableBinding binding : inheritedBindings) {
            Node boundNode = binding.getBoundNode();
            if (BLEND.unbound.equals(boundNode)) {
                continue;
            }
            Node variable = binding.getVariable();
            currentBindings.add(new Triple(variable, BLEND.boundTo, boundNode));
        }
        return new Union(
                        new Union(
                                        currentBindings,
                                        new Union(
                                                        instance.leftTemplate.getTemplateGraphs().getBindingsGraph(),
                                                        instance.rightTemplate.getTemplateGraphs().getBindingsGraph())),
                        instance.blendingBackground.combineWithBackgroundData(
                                        getBlendedGraphs()));
    }

    public Graph getBlendedGraphs() {
        Graph blended = GraphFactory.createDefaultGraph();
        instance.leftTemplate.getTemplateGraphs().getDataGraph().find().mapWith(t -> blendTriple(t, inheritedBindings))
                        .forEach(blended::add);
        instance.rightTemplate.getTemplateGraphs().getDataGraph().find().mapWith(t -> blendTriple(t, inheritedBindings))
                        .forEach(blended::add);
        blended.getPrefixMapping()
                        .setNsPrefixes(instance.leftTemplate.getTemplateGraphs().getDataGraph().getPrefixMapping());
        return new VariableAwareGraph(blended, this::isVariable);
    }

    private Triple blendTriple(Triple t, List<VariableBinding> bindings) {
        Triple blended = new Triple(
                        dereferenceIfVariable(t.getSubject(), bindings),
                        dereferenceIfVariable(t.getPredicate(), bindings),
                        dereferenceIfVariable(t.getObject(), bindings));
        return blended;
    }

    public Optional<VariableBinding> getBinding(Node node) {
        for (VariableBinding b : inheritedBindings) {
            if (b.getVariable().equals(node)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    public BindingExtractionState invertCheckedBindingsValidity() {
        return new BindingExtractionState(
                        inheritedBindings,
                        checkedBindings.stream().map(CheckedBindings::invertValidity).collect(toSet()),
                        node,
                        valueNodes,
                        instance);
    }

    public BindingExtractionState resolveValidVsInvalid() {
        return new BindingExtractionState(inheritedBindings,
                        resolveCheckedBindings(checkedBindings),
                        node, valueNodes, instance);
    }

    public static Set<CheckedBindings> resolveCheckedBindings(Set<CheckedBindings> checkedBindings) {
        return checkedBindings.stream()
                        .filter(c -> checkedBindings.stream().noneMatch(o -> c.hasOppositeValidityOf(o)))
                        .filter(c -> {
                            boolean noStrongerVersion = c.hasKnownValidity()
                                            || checkedBindings.stream().noneMatch(o -> {
                                                boolean ret = o.isStrongerValidityThan(c);
                                                return ret;
                                            });
                            return noStrongerVersion;
                        })
                        .collect(toSet());
    }

    public boolean isBindingAllowed(Node node, Node newlyBoundNode) {
        if (!isVariable(node)) {
            return false;
        }
        BindingExtractionState s = addBinding(node, newlyBoundNode);
        return s.inheritedBindings.stream()
                        .allMatch(b -> isValidReferenceChainLength(b.getVariable(), s.inheritedBindings));
    }

    public boolean alreadyDecidedVariable(Node node) {
        return getBinding(node).isPresent();
    }

    public boolean isUnboundOrUnsetVariable(Node node) {
        if (!isVariable(node)) {
            return false;
        }
        Optional<VariableBinding> boundNode = getBinding(node);
        if (boundNode.isEmpty()) {
            return true;
        }
        return boundNode.get().getBoundNode().equals(BLEND.unbound);
    }

    public boolean containsUnboundOrUnsetVariable(Collection<Node> nodes) {
        return nodes.stream().anyMatch(this::isUnboundOrUnsetVariable);
    }

    public BindingExtractionState keepOnlyInvalidBindings() {
        return new BindingExtractionState(inheritedBindings,
                        checkedBindings.stream().filter(CheckedBindings::isKnownInvalid).collect(
                                        Collectors.toUnmodifiableSet()),
                        node, valueNodes, instance);
    }

    public BindingExtractionState consolidateCheckedBindings() {
        Set<CheckedBindings> consolidated = consolidate(checkedBindings);
        return new BindingExtractionState(
                        inheritedBindings,
                        consolidated,
                        node,
                        valueNodes,
                        instance);
    }

    public static Set<CheckedBindings> consolidate(Set<CheckedBindings> cb) {
        Map<List<VariableBinding>, Set<CheckedBindings>> grouped = cb
                        .stream()
                        .collect(Collectors.groupingBy(c -> c.bindings, toSet()));
        Set<CheckedBindings> consolidated = grouped.entrySet().stream().map(e -> {
            Ternary validity = e.getValue().stream().map(c -> c.valid).reduce(TRUE, (l, r) -> Ternary.and(l, r));
            return new CheckedBindings(e.getKey(),
                            validity);
        }).collect(toSet());
        return consolidated;
    }

    public BindingExtractionState reduceWithAnd() {
        Ternary validity = checkedBindings.stream().map(cb -> cb.valid).reduce(TRUE, (l, r) -> Ternary.and(l, r));
        return this.setValidity(validity, this);
    }

    public boolean containsUnknownValidityBindings() {
        return checkedBindings.stream().anyMatch(c -> c.valid.isUnknown());
    }

    public boolean containsInvalidBindings() {
        return checkedBindings.stream().anyMatch(c -> c.valid.isFalse());
    }

    public boolean containsOnlyInvalidBindings() {
        return checkedBindings.stream().allMatch(c -> c.valid.isFalse());
    }

    public boolean containsNoValidBindings() {
        return checkedBindings.stream().map(b -> b.valid).allMatch(v -> v.isFalse() || v.isUnknown());
    }

    public boolean containsOnlyValidBindings() {
        return checkedBindings.stream().allMatch(c -> c.valid.isTrue());
    }

    public boolean containsNoInvalidBindings() {
        return checkedBindings.stream().noneMatch(c -> c.valid.isFalse());
    }

    public BindingExtractionState setValidity(Ternary validity, BindingExtractionState subState) {
        return new BindingExtractionState(
                        inheritedBindings,
                        Set.of(new CheckedBindings(inheritedBindings, validity)),
                        node,
                        valueNodes,
                        instance);
    }

    public BindingExtractionState setValidity(Ternary validity) {
        return setValidity(validity, this);
    }

    public BindingExtractionState valid() {
        return valid(this);
    }

    public BindingExtractionState valid(BindingExtractionState subState) {
        return setValidity(TRUE, subState);
    }

    public BindingExtractionState invalid() {
        return invalid(this);
    }

    public BindingExtractionState invalid(BindingExtractionState subState) {
        return setValidity(FALSE, subState);
    }

    public BindingExtractionState unknownValidity() {
        return unknownValidity(this);
    }

    public BindingExtractionState unknownValidity(BindingExtractionState subState) {
        return setValidity(UNKNOWN, subState);
    }
}
