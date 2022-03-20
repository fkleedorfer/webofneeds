package won.utils.blend.algorithm.support.bindings.options;

import org.apache.jena.graph.Node;
import won.utils.blend.BlendingOptions;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;

import java.util.Set;

public class InstanceBindingOptionProvider implements BindingOptionsProvider {
    private final BlendingInstance blendingInstance;
    private final BlendingInstanceLogic blendingInstanceLogic;

    public InstanceBindingOptionProvider(BlendingInstance blendingInstance) {
        this.blendingInstance = blendingInstance;
        this.blendingInstanceLogic = new BlendingInstanceLogic(blendingInstance);
    }

    @Override
    public Set<Node> apply(Node node) {
        return blendingInstanceLogic.getBindingOptions(node);
    }
}
