package won.utils.blend.support.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class GraphAccessObserver {
    private Function<Node, Boolean> isVariableCheck;
    private VariableAwareGraph wrapper;
    private Set<Node> encounteredVariables = new HashSet<>();

    public GraphAccessObserver(Function<Node, Boolean> isVariableCheck) {
        this.isVariableCheck = isVariableCheck;
    }

    public Graph wrap(Graph graph) {
        getEncounteredVariablesFromWrapper();
        wrapper = new VariableAwareGraph(graph, isVariableCheck);
        return wrapper;
    }

    public void getEncounteredVariablesFromWrapper() {
        if (wrapper != null) {
            encounteredVariables.addAll(wrapper.getEncounteredVariables());
        }
    }

    public void reset() {
        wrapper = null;
        encounteredVariables.clear();
    }
}
