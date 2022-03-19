package won.utils.blend.algorithm.support;

import org.apache.jena.graph.Graph;

import static won.utils.blend.algorithm.support.BlendingUtils.combineGraphsIfPresent;

public class BlendingBackground {
    private Graph shapes;
    private Graph data;

    public BlendingBackground(Graph shapes, Graph data) {
        this.shapes = shapes;
        this.data = data;
    }

    public Graph combineWithBackgroundData(Graph dataGraph) {
        return combineGraphsIfPresent(dataGraph, this.data);
    }

    public Graph combineWithBackgroundShapes(Graph shapesGraph) {
        return combineGraphsIfPresent(shapesGraph, this.shapes);
    }

    public Graph getShapes() {
        return shapes;
    }

    public Graph getData() {
        return data;
    }
}
