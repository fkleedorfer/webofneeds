package won.utils.blend.algorithm.support;

import org.apache.jena.graph.Graph;

public class AltGraphValidationContext {
    public final Graph alternativeGraph;

    public AltGraphValidationContext(Graph dataGraph) {
        this.alternativeGraph = dataGraph;
    }
}
