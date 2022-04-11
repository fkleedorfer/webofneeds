package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Node;

public class ShapeFocusNode {
    public final Node shape;
    public final Node focusNode;

    public ShapeFocusNode(Node shape, Node focusNode) {
        this.shape = shape;
        this.focusNode = focusNode;
    }
}
