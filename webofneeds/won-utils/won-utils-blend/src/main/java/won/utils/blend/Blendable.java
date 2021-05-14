package won.utils.blend;

import org.apache.jena.graph.Graph;

public class Blendable {
    public Graph dataGraph;
    public Graph shapesGraph;

    public Blendable(Graph dataGraph, Graph shapesGraph) {
        this.dataGraph = dataGraph;
        this.shapesGraph = shapesGraph;
    }
}