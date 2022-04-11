package won.utils.blend.support.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import java.util.Set;

public interface VariableAwareGraph extends Graph {
    void resetEncounteredVariables();

    Set<Node> getEncounteredVariables();
}
