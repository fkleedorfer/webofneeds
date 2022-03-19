package won.utils.blend.support.stats;

import org.apache.jena.graph.Node;

public class TemplateStats {
    public final Node templateNode;
    public final BindingStats bindingStats;

    public TemplateStats(Node templateNode, BindingStats bindingStats) {
        this.templateNode = templateNode;
        this.bindingStats = bindingStats;
    }

    public Node getTemplateNode() {
        return templateNode;
    }

    public BindingStats getBindingStats() {
        return bindingStats;
    }
}
