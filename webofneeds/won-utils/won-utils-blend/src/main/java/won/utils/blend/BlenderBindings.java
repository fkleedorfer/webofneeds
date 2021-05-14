package won.utils.blend;

import org.apache.jena.graph.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BlenderBindings implements Cloneable {
    public Map<Node, Node> bindings;

    public BlenderBindings() {
        this.bindings = new HashMap<>();
    }

    public void addBinding(Node variable, Node constant) {
        if (bindings.containsKey(variable)) {
            throw new IllegalArgumentException(String.format("Binding for %s already exists", variable.toString()));
        }
        bindings.put(variable, constant);
    }

    public boolean hasBindingForVariable(Node variable) {
        return bindings.containsKey(variable);
    }

    public Object clone() {
        BlenderBindings clone = null;
        try {
            clone = (BlenderBindings) super.clone();
        } catch (CloneNotSupportedException e) {
            // swallow
        }
        clone.bindings = new HashMap<>();
        clone.bindings.putAll(bindings);
        return clone;
    }

    @Override
    public String toString() {
        return "BlenderBindings{" +
                        "bindings=" + bindings +
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
        BlenderBindings bindings1 = (BlenderBindings) o;
        return Objects.equals(bindings, bindings1.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindings);
    }
}
