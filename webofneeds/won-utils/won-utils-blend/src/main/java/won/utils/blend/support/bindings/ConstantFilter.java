package won.utils.blend.support.bindings;

import org.apache.jena.graph.Node;

public interface ConstantFilter {
    public boolean accept(Node constant);
}
