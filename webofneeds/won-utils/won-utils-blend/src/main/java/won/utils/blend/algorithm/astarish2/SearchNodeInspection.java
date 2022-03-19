package won.utils.blend.algorithm.astarish2;

import org.apache.jena.shacl.ValidationReport;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.graph.TemplateGraphs;

import java.util.Set;

class SearchNodeInspection {
    SearchNode2 node;
    Set<VariableBinding> bindings;
    TemplateBindings templateBindings;
    TemplateGraphs blendingResult;
    ValidationReport validationReport;
    float score;
    boolean acceptable;
    int graphSize;

    public SearchNodeInspection(SearchNode2 node) {
        this.node = node;
    }
}
