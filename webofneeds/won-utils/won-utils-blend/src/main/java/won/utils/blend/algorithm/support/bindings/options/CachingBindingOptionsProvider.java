package won.utils.blend.algorithm.support.bindings.options;

import org.apache.jena.graph.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CachingBindingOptionsProvider extends DelegatingBindingOptionsProvider {
    private Map<Node, Set<Node>> cache = new HashMap<>();

    public CachingBindingOptionsProvider(BindingOptionsProvider delegate) {
        super(delegate);
    }

    @Override public Set<Node> apply(Node node) {
        Set<Node> values = cache.get(node);
        if (values == null) {
            values = getDelegate().apply(node);
            if (values != null) {
                cache.put(node, values);
            }
        }
        return values;
    }
}