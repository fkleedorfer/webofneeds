package won.utils.blend.support.shacl;

import org.apache.jena.graph.*;
import org.apache.jena.riot.other.G;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.RDF;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class ShapeUtils {
    public static Map<Node, Graph> extractShapeGraphs(Graph shapesGraph) {
        if (shapesGraph == null) {
            return Map.of();
        }
        Set<Node> shapeNodes = G.allPO(shapesGraph, RDF.type.asNode(), SHACL.NodeShape);
        shapeNodes.addAll(G.allPO(shapesGraph, RDF.type.asNode(), SHACL.PropertyShape));
        Map<Node, Graph> shapeNodetoShapeGraph = new HashMap<>();
        for (Node shapeNode : shapeNodes) {
            Graph extractedShapeGraph = new GraphExtract(new TripleBoundary() {
                @Override
                public boolean stopAt(Triple t) {
                    Node predicate = t.getPredicate();
                    String predicateUri = predicate.getURI();
                    boolean isShaclPredicate = predicateUri.startsWith(SHACL.getURI());
                    if (isShaclPredicate) {
                        return false;
                    }
                    if (RDF.first.equals(predicate)) {
                        return false;
                    }
                    if (RDF.rest.equals(predicate)) {
                        return false;
                    }
                    return true;
                }
            }).extract(shapeNode, shapesGraph);
            shapeNodetoShapeGraph.put(shapeNode, extractedShapeGraph);
        }
        return shapeNodetoShapeGraph;
    }
}
