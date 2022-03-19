package won.utils.blend.algorithm.sat.shacl;

import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.ShapeVisitor;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class ValidatingShapeVisitor implements ShapeVisitor {
    private Deque<BindingExtractionState> stateStack = new ArrayDeque<>();
    private Set<Set<VariableBinding>> collectedBindingSets = new HashSet<>();

    @Override
    public void visit(NodeShape nodeShape) {
    }

    @Override
    public void visit(PropertyShape propertyShape) {
    }
}
