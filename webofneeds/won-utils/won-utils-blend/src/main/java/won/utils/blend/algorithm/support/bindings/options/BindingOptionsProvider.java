package won.utils.blend.algorithm.support.bindings.options;

import org.apache.jena.graph.Node;

import java.util.Set;
import java.util.function.Function;

public interface BindingOptionsProvider extends Function<Node, Set<Node>> {
}
