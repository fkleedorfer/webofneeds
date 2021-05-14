package won.utils.blend;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

public class BLEND {
    public static final String baseUri = "https://w3id.org/won/blending#";
    public static final Node Variable = uri("Variable");
    public static final Node BlendingDisabled = uri("BlendingDisabled");
    public static final Node name = uri("name");
    public static final Node Blendable = uri("Blendable");
    public static final Node dataGraph = uri("dataGraph");
    public static final Node shapesGraph = uri("shapesGraph");

    private static Node uri(String variable) {
        return NodeFactory.createURI(baseUri + variable);
    }
}
