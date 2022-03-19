package won.utils.blend;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

public abstract class EX {
    public static final String baseUri = "http://example.org/ns#";

    public static Node uri(String variable) {
        return NodeFactory.createURI(baseUri + variable);
    }
}
