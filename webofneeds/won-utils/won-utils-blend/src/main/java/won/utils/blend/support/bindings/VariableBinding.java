package won.utils.blend.support.bindings;

import org.apache.jena.graph.Node;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class VariableBinding implements Comparable<VariableBinding> {
    private Node variable;
    private Node boundNode;
    private boolean boundNodeIsVariable;

    public VariableBinding(Node variable, Node boundNode) {
        requireNonNull(variable);
        requireNonNull(boundNode);
        this.variable = variable;
        this.boundNode = boundNode;
        this.boundNodeIsVariable = false;
    }

    public VariableBinding(Node variable, Node boundNode, boolean boundNodeIsVariable) {
        this.variable = variable;
        this.boundNode = boundNode;
        this.boundNodeIsVariable = boundNodeIsVariable;
    }

    @Override
    public int compareTo(VariableBinding o) {
        int cmp = 0;
        cmp = variable.toString().compareTo(o.variable.toString());
        if (cmp == 0) {
            cmp = boundNode.toString().compareTo(o.boundNode.toString());
        }
        return cmp;
    }

    public Node getVariable() {
        return variable;
    }

    public Node getBoundNode() {
        return boundNode;
    }

    public boolean isBoundNodeVariable() {
        return boundNodeIsVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VariableBinding that = (VariableBinding) o;
        return Objects.equals(variable, that.variable) &&
                        Objects.equals(boundNode, that.boundNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variable, boundNode);
    }

    @Override
    public String toString() {
        return "Binding{" +
                        variable + " -> " + boundNode +
                        '}';
    }
}
