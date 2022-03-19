package won.utils.blend.algorithm.sat.shacl;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.sat.support.Ternary;

import static java.util.stream.Collectors.joining;

public abstract class ProcessingStepMessages {
    public static String initialShapeMsg(Shape shape) {
        return String.format("initial NodeShape: %s", shape.getShapeNode());
    }

    public static String initialFocusNodeMsg(Node node) {
        return String.format("initial focus node: %s", node);
    }

    public static String processingNodeShapeMsg(Shape shape, Node focusNode) {
        return String.format("NodeShape %s on focus node %s", shape.getShapeNode(), focusNode);
    }

    public static String processingPropertyShapeMsg(PropertyShape propertyShape, Node focusNode) {
        return String.format("PropertyShape %s on focus node %s", propertyShape.getShapeNode(), focusNode);
    }

    public static String processPropertyShapeConstraintMsg(
                    Class<? extends Constraint> constraintClass, BindingExtractionState state) {
        return String.format("PropertyShape constraint %s: %s", constraintClass.getSimpleName(),
                        state.checkedBindings.stream().map(cb -> cb.valid).map(Object::toString).collect(joining(",")));
    }

    public static String processNodeShapeConstraintMsg(NodeShape nodeShape, Node focusNode,
                    Class<? extends Constraint> constraintClass, BindingExtractionState state) {
        return String.format("NodeShape %s constraint %s on focusNode %s: %s", nodeShape,
                        constraintClass.getSimpleName(), focusNode,
                        state.checkedBindings.stream().map(cb -> cb.valid).map(Object::toString).collect(joining(",")));
    }

    public static String handlingPropShapeConstrintAsNodeShapeConstraintMsg(Node focusNode,
                    Class<? extends Constraint> constratintClass) {
        return String.format("%s: handling PropertyShapeConstraint as NodeShapeConstraint on %s",
                        constratintClass.getSimpleName(), focusNode);
    }

    public static String setValidityMessage(Ternary validity) {
        return String.format("validity decision: %s", validity);
    }
}
