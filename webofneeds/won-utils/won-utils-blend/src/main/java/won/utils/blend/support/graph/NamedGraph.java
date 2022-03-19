package won.utils.blend.support.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.graph.UnmodifiableGraph;

public class NamedGraph {
    public final Node node;
    public final Graph graph;

    public NamedGraph(Node node, Graph graph) {
        this.node = node;
        this.graph = graph == null ? null : new UnmodifiableGraph(graph);
    }
}
