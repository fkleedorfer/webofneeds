package won.utils.blend.algorithm.support.bindings.options;

import org.apache.jena.graph.Node;

import java.util.Set;

public class DelegatingBindingOptionsProvider implements BindingOptionsProvider {
    private BindingOptionsProvider delegate;

    public DelegatingBindingOptionsProvider(
                    BindingOptionsProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<Node> apply(Node node) {
        return delegate.apply(node);
    }

    protected BindingOptionsProvider getDelegate() {
        return delegate;
    }
}
