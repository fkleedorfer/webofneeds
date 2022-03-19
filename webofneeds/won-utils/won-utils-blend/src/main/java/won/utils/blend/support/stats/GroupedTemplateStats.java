package won.utils.blend.support.stats;

import org.apache.jena.graph.Node;

import java.util.Set;

public class GroupedTemplateStats {
    public final int count;
    public Set<Node> templateNodes;
    public BindingStats bindingStats;

    public GroupedTemplateStats(Set<Node> templateNodes, BindingStats bindingStats) {
        this.templateNodes = templateNodes;
        this.bindingStats = bindingStats;
        this.count = templateNodes.size();
    }
}
