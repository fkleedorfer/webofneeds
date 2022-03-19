package won.utils.blend;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

public class BLEND {
    public static final String baseUri = "https://w3id.org/won/blending#";
    public static final Node Variable = uri("Variable");
    public static final Node Unblendable = uri("Unblendable");
    public static final Node candidateShape = uri("candidateShape");
    public static final Node name = uri("name");
    public static final Node Template = uri("Template");
    public static final Node dataGraph = uri("dataGraph");
    public static final Node bindingsGraph = uri("bindingsGraph");
    public static final Node blendingConfigGraph = uri("blendingConfigGraph");
    public static final Node shapesGraph = uri("shapesGraph");
    public static final Node boundTo = uri("boundTo");
    public static final Node unbound = uri("unbound");
    public static Node BlankNodeVariable = uri("BlankNodeVariable");

    private static Node uri(String variable) {
        return NodeFactory.createURI(baseUri + variable);
    }
}
