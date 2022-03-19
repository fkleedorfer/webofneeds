package won.utils.blend.support.shacl;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;

import java.util.Objects;

public class ShapeInShapes {
    private final Shapes shapes;
    private final Node shapeNode;

    public ShapeInShapes(Shapes shapes, Node shapeNode) {
        this.shapes = shapes;
        this.shapeNode = shapeNode;
        Objects.requireNonNull(shapes.getShape(shapeNode));
    }

    public Shapes getShapes() {
        return shapes;
    }

    public Shape getShape(){
        return shapes.getShape(shapeNode);
    }

    public Node getShapeNode() {
        return shapeNode;
    }
}
