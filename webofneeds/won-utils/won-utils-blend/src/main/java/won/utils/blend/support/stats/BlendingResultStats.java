package won.utils.blend.support.stats;

import org.apache.jena.graph.Node;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.Map;
import java.util.Set;

public class BlendingResultStats {
    public final Map<Node, Set<Node>> boundValuesByVariable;
    public final int results;
    public final Set<Node> variables;
    public final Set<Node> constants;
    public final Set<VariableBinding> fixedBindings;
    public final Set<GroupedTemplateStats> groupedTemplateStats;
    public final Set<VariableBindings> allBindings;

    public BlendingResultStats(
                    Map<Node, Set<Node>> boundValuesByVariable, int results,
                    Set<Node> variables, Set<Node> constants, Set<VariableBinding> fixedBindings,
                    Set<GroupedTemplateStats> groupedTemplateStats, Set<VariableBindings> allBindings) {
        this.boundValuesByVariable = boundValuesByVariable;
        this.results = results;
        this.variables = variables;
        this.constants = constants;
        this.fixedBindings = fixedBindings;
        this.groupedTemplateStats = groupedTemplateStats;
        this.allBindings = allBindings;
    }
}
