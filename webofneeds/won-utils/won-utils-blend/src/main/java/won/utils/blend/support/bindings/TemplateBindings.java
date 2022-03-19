package won.utils.blend.support.bindings;

import org.apache.jena.graph.Node;
import won.utils.blend.BLEND;
import won.utils.blend.Template;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class TemplateBindings {
    private VariableBindings variableBindings;
    private Template leftTemplate;
    private Template rightTemplate;
    private final Set<Node> allVariables;
    private final Set<Node> allConstants;

    public TemplateBindings(Template leftTemplate, Template rightTemplate) {
        this(leftTemplate, rightTemplate, Set.of());
    }

    public TemplateBindings(Template leftTemplate, Template rightTemplate, Set<VariableBinding> initialBindings) {
        this.leftTemplate = leftTemplate;
        this.rightTemplate = rightTemplate;
        Stream.concat(
                        leftTemplate.getFixedBindings().getBindingsAsSet().stream(),
                        rightTemplate.getFixedBindings().getBindingsAsSet().stream())
                        .forEach(this::addBinding);
        Set<Node> tmpSet = new HashSet<>();
        tmpSet.addAll(leftTemplate.getVariables());
        tmpSet.addAll(rightTemplate.getVariables());
        this.allVariables = Collections.unmodifiableSet(tmpSet);
        tmpSet = new HashSet<>();
        tmpSet.addAll(leftTemplate.getConstants());
        tmpSet.addAll(rightTemplate.getConstants());
        this.allConstants = Collections.unmodifiableSet(tmpSet);
        this.variableBindings = new VariableBindings(this.allVariables, initialBindings);
    }

    public TemplateBindings(Template leftTemplate, Template rightTemplate, VariableBindings initialBindings) {
        this.leftTemplate = leftTemplate;
        this.rightTemplate = rightTemplate;
        Stream.concat(
                                        leftTemplate.getFixedBindings().getBindingsAsSet().stream(),
                                        rightTemplate.getFixedBindings().getBindingsAsSet().stream())
                        .forEach(this::addBinding);
        Set<Node> tmpSet = new HashSet<>();
        tmpSet.addAll(leftTemplate.getVariables());
        tmpSet.addAll(rightTemplate.getVariables());
        this.allVariables = Collections.unmodifiableSet(tmpSet);
        tmpSet = new HashSet<>();
        tmpSet.addAll(leftTemplate.getConstants());
        tmpSet.addAll(rightTemplate.getConstants());
        this.allConstants = Collections.unmodifiableSet(tmpSet);
        this.variableBindings = new VariableBindings(initialBindings);
        if (!initialBindings.getVariables().equals(this.allVariables)){
            throw new IllegalArgumentException("Cannot use bindings with a different set of variables from that of the template: " + variableBindings.getVariables());
        }
    }

    public void addBinding(VariableBinding variableBinding) {
        if (variableBindings.addBindingIfPossible(variableBinding.getVariable(), variableBinding.getBoundNode())){
            throw new IllegalArgumentException(
                            String.format("Cannot add binding %s", variableBinding.toString()));
        }
    }

    public boolean hasBindingForVariable(Node variable) {
        return variableBindings.isBoundVariable(variable);
    }


    public boolean hasConstantBindingForVariableAllowTransitive(Node variable) {
        Optional<Node> boundNode = variableBindings.dereferenceIfVariable(variable);
        if (boundNode.isEmpty()) {
            return false;
        }
        return true;
    }

    public Optional<Node> getBoundNodeForVariableAllowTransitive(Node variable) {
        return  variableBindings.dereferenceIfVariable(variable);
    }


    public boolean isVariable(Node variable) {
        return allVariables.contains(variable);
    }

    public boolean isConstant(Node constant) {
        return allConstants.contains(constant);
    }

    public Set<Node> getVariables() {
        return allVariables;
    }

    public Set<Node> getBoundVariables() {
        return variableBindings.getBoundVariables();
    }

    public boolean hasUnboundVariables() {
        return !variableBindings.isAllVariablesBound();
    }

    public Set<Node> getUnboundVariables() {
        return variableBindings.getUnboundVariables();
    }

    public Optional<Node> getBoundNodeForVariable(Node variable) {
        return variableBindings.getBoundNode(variable);
    }

    public VariableBindings getBindings() {
        return new ImmutableVariableBindings(variableBindings);
    }

    public Template getLeftTemplate() {
        return leftTemplate;
    }

    public Template getRightTemplate() {
        return rightTemplate;
    }

    @Override
    public String toString() {
        return "TemplateBinding{ " +
                        variableBindings.size() + " bindings: "
                        + getBindings().getBindingsAsSet().stream()
                                        .map(e -> e.getVariable() + " = " + e.getBoundNode())
                                        .collect(Collectors.joining("\n\t", "{\n\t", "\n}"))
                        +
                        '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TemplateBindings bindings1 = (TemplateBindings) o;
        return Objects.equals(variableBindings, bindings1.variableBindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableBindings);
    }

    public boolean isBoundToVariable(Node variable) {
        return getBoundNodeForVariable(variable).map(n -> isVariable(n)).orElse(false);
    }
}
