package won.utils.blend.algorithm.sat.shacl2;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.sat.support.BindingValidity;
import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.Collections;
import java.util.Set;

public class BindingValidationResult {
    public final VariableBindings bindings;
    public final Set<Node> encounteredVariables;
    public final Ternary valid;
    public final Ternary globallyValid;
    public final Node node;
    public final Shape shape;

    public BindingValidationResult(Shape shape, Node focusNode, VariableBindings bindings,
                    Set<Node> encounteredVariables, Ternary valid, Ternary globallyValid) {
        this.node = focusNode;
        this.shape = shape;
        this.bindings = new VariableBindings(bindings);
        this.encounteredVariables = Collections.unmodifiableSet(encounteredVariables);
        this.valid = valid;
        this.globallyValid = globallyValid;
    }

    public BindingValidity toBindingValidity() {
        return new BindingValidity(valid, bindings.getBindingsAsSet());
    }
}
