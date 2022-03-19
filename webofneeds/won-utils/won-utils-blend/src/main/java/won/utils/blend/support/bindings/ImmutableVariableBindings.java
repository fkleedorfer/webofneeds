package won.utils.blend.support.bindings;

import org.apache.jena.graph.Node;

import java.util.Map;
import java.util.Set;

public class ImmutableVariableBindings extends VariableBindings {
    public ImmutableVariableBindings(VariableBindings toCopy) {
        super(toCopy);
    }

    public ImmutableVariableBindings(Set<Node> variables) {
        super(variables);
    }

    public ImmutableVariableBindings(Set<Node> variables,
                    Set<VariableBinding> fromBindings) {
        super(variables, fromBindings);
    }

    public ImmutableVariableBindings(Set<Node> variables,
                    Map<Node, Node> bindings) {
        super(variables, bindings);
    }

    @Override public void setAll(Set<VariableBinding> fromBindings) {
        throw new UnsupportedOperationException("Cannot modify: this VariableBindings object is immutable");
    }

    @Override public void setAll(VariableBindings bindings) {
        throw new UnsupportedOperationException("Cannot modify: this VariableBindings object is immutable");
    }

    @Override public boolean addBindingIfPossible(Node node, Node newlyBoundNode) {
        throw new UnsupportedOperationException("Cannot modify: this VariableBindings object is immutable");
    }
}
