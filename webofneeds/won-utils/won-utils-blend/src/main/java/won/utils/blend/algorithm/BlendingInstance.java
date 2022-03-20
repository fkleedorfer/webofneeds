package won.utils.blend.algorithm;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.other.G;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.vocabulary.SHACL;
import won.utils.blend.BLEND;
import won.utils.blend.BlendingOptions;
import won.utils.blend.Template;
import won.utils.blend.algorithm.support.BlendingBackground;
import won.utils.blend.algorithm.support.BlendingOperations;
import won.utils.blend.algorithm.support.BlendingResultEvaluator;
import won.utils.blend.support.shacl.ShapeInShapes;

import java.util.*;

import static won.utils.blend.algorithm.support.BlendingUtils.combineGraphsIfPresent;
import static won.utils.blend.support.shacl.ShapeUtils.extractShapeGraphs;

public class BlendingInstance {
    public final BlendingOperations blendingOperations;
    public final BlendingBackground blendingBackground;
    public final BlendingResultEvaluator blendingResultEvaluator;
    public final Template leftTemplate, rightTemplate;
    public final BlendingOptions blendingOptions;
    public final Shapes shapes;
    public final Map<Node, Set<ShapeInShapes>> candidateShapesByVariable;

    public BlendingInstance(BlendingOperations blendingOperations, BlendingBackground blendingBackground,
                    Template leftTemplate,
                    Template rightTemplate, BlendingOptions blendingOptions) {
        this.blendingOperations = blendingOperations;
        this.blendingResultEvaluator = new BlendingResultEvaluator(blendingBackground);
        this.leftTemplate = leftTemplate;
        this.rightTemplate = rightTemplate;
        this.blendingOptions = blendingOptions;
        this.blendingBackground = blendingBackground;
        this.shapes = Shapes.parse(
                        blendingBackground.combineWithBackgroundShapes(
                                        combineGraphsIfPresent(
                                                        leftTemplate.getTemplateGraphs().getShapesGraph(),
                                                        rightTemplate.getTemplateGraphs().getShapesGraph())));
        this.candidateShapesByVariable = Collections.unmodifiableMap(collectCandidateShapesFromBothTemplates());
    }

    private Map<Node, Set<ShapeInShapes>> collectCandidateShapesFromBothTemplates() {
        Map<Node, Set<ShapeInShapes>> candidateShapes = new HashMap<>();
        Map<Node, ShapeInShapes> parsedCandidateShapes = new HashMap<>();
        Map<Node, Graph> shapeGraphs = extractShapeGraphs(blendingBackground.getShapes());
        candidateShapes.putAll(collectCandidateShapes(leftTemplate, shapeGraphs, parsedCandidateShapes));
        candidateShapes.putAll(collectCandidateShapes(rightTemplate, shapeGraphs, parsedCandidateShapes));
        return candidateShapes;
    }

    private static Map<Node, Set<ShapeInShapes>> collectCandidateShapes(Template template,
                    Map<Node, Graph> backgroundShapeGraphs, Map<Node, ShapeInShapes> parsedCandidateShapes) {
        Map<Node, Set<ShapeInShapes>> variablesToCandidateShapes = new HashMap<>();
        for (Node variable : template.getVariables()) {
            Set<ShapeInShapes> candidateShapes = new HashSet<>();
            Set<Node> candidateShapeNodes = G.allSP(template.getTemplateGraphs().getBlendingConfigGraph(), variable,
                            BLEND.candidateShape);
            for (Node shapeNode : candidateShapeNodes) {
                ShapeInShapes candidateShape = parsedCandidateShapes.get(shapeNode);
                if (candidateShape == null) {
                    Graph shapeGraph = template.getShapeGraphs().get(shapeNode);
                    if (shapeGraph == null) {
                        shapeGraph = backgroundShapeGraphs.get(shapeNode);
                    }
                    if (shapeGraph == null) {
                        throw new IllegalArgumentException("Shape not found: " + shapeNode);
                    }
                    shapeGraph.delete(shapeNode, SHACL.deactivated,
                                    NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
                    Shapes shapes = Shapes.parse(shapeGraph);
                    candidateShape = new ShapeInShapes(shapes, shapeNode);
                    if (candidateShape == null) {
                        throw new IllegalStateException("No shape found after parsing: " + shapeNode);
                    }
                    parsedCandidateShapes.put(shapeNode, candidateShape);
                }
                if (candidateShape.getShape().deactivated()) {
                    throw new IllegalStateException("Shape must not be deactivated: " + candidateShape);
                }
                candidateShapes.add(candidateShape);
            }
            variablesToCandidateShapes.put(variable, candidateShapes);
        }
        return variablesToCandidateShapes;
    }
}
