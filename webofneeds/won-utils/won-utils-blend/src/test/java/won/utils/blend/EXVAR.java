package won.utils.blend;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

public abstract class EXVAR {
    public static final String baseUri = "http://example.org/var#";

    public static Node uri(String variable) {
        return NodeFactory.createURI(baseUri + variable);
    }
}
