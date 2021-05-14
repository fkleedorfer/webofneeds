package won.utils.blend;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.RDF;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BlendableLoader {
    public static Set<Blendable> load(DatasetGraph datasetGraph) {
        Set<Blendable> result = new HashSet<>();
        Set<Node> blendableNodes = findBlendables(datasetGraph);
        for (Node blendableNode : blendableNodes) {
            Blendable blendable = makeBlendable(datasetGraph, blendableNode);
            result.add(blendable);
        }
        return result;
    }

    public static Blendable makeBlendable(DatasetGraph datasetGraph, Node blendable) {
        Set<Node> dataGraphNodes = findByPredicate(datasetGraph, blendable, BLEND.dataGraph);
        if (dataGraphNodes.size() > 1) {
            throw new IllegalStateException(String.format("Blendable %s has more than one dataGraphs"));
        }
        Set<Node> shapesGraphNodes = findByPredicate(datasetGraph, blendable, BLEND.shapesGraph);
        if (shapesGraphNodes.size() > 1) {
            throw new IllegalStateException(String.format("Blendable %s has more than one shapesGraph"));
        }
        Graph dataGraph = null;
        if (!dataGraphNodes.isEmpty()) {
            dataGraph = datasetGraph.getGraph(dataGraphNodes.stream().findFirst().get());
        }
        Graph shapesGraph = null;
        if (!shapesGraphNodes.isEmpty()) {
            shapesGraph = datasetGraph.getGraph(shapesGraphNodes.stream().findFirst().get());
        }
        Blendable result = new Blendable(dataGraph, shapesGraph);
        return result;
    }

    private static Set<Node> findBlendables(DatasetGraph datasetGraph) {
        Set<Node> nodes = new HashSet<>();
        Iterator<Quad> it = datasetGraph.find(null, null, RDF.type.asNode(), BLEND.Blendable);
        while (it.hasNext()) {
            nodes.add(it.next().getSubject());
        }
        return nodes;
    }

    private static Set<Node> findByPredicate(DatasetGraph datasetGraph, Node subject, Node predicate) {
        Set<Node> nodes = new HashSet<>();
        Iterator<Quad> it = datasetGraph.find(null, subject, predicate, null);
        while (it.hasNext()) {
            nodes.add(it.next().getObject());
        }
        return nodes;
    }
}
