package won.utils.blend.support.bindings;

import org.apache.jena.graph.Node;

public class UriPrefixConstantFilter implements ConstantFilter {
    final private String prefix;

    public UriPrefixConstantFilter(String prefix, boolean accept) {
        this.prefix = prefix;
    }

    @Override
    public boolean accept(Node constant) {
        if (!constant.isURI()) {
            return true;
        }
        return constant.getURI().startsWith(prefix);
    }
}
