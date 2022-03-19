package won.utils.blend;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.iri.IRI;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.bruteforce.BruteForceBlendingAlgorithm;
import won.utils.blend.algorithm.support.BlendingBackground;
import won.utils.blend.algorithm.support.BlendingOperations;
import won.utils.blend.algorithm.support.BlendingResultEvaluator;
import won.utils.blend.support.graph.NamedGraph;
import won.utils.blend.support.graph.TemplateGraphs;
import won.utils.blend.support.io.TemplateIO;
import won.utils.blend.support.uuid.RandomUUIDSource;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class Blender {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private BlendingOperations blendingOperations;
    private BlendingAlgorithm blendingAlgorithm;
    private BlendingBackground blendingBackground;
    private BlendingResultEvaluator blendingResultEvaluator;

    public Blender(Graph globalShapesGraph, Graph globalDataGraph) {
        this(new BruteForceBlendingAlgorithm(),
                        new BlendingOperations(new TemplateIO(new RandomUUIDSource())),
                        new BlendingBackground(globalShapesGraph, globalDataGraph));
    }

    public Blender(BlendingAlgorithm blendingAlgorithm,
                    BlendingOperations blendingOperations,
                    BlendingBackground blendingBackground) {
        this.blendingOperations = blendingOperations;
        this.blendingAlgorithm = blendingAlgorithm;
        this.blendingBackground = blendingBackground;
        this.blendingResultEvaluator = new BlendingResultEvaluator(blendingBackground);
    }

    public Stream<TemplateGraphs> blendToTemplateGraphs(Template left, Template right) {
        return blendToTemplateGraphs(left, right, new BlendingOptions());
    }

    public Stream<Template> blend(Template left, Template right) {
        return blend(left, right, new BlendingOptions());
    }

    public Stream<Template> blend(Template left, Template right, BlendingOptions blendingOptions) {
        return blendToTemplateGraphs(left, right, blendingOptions)
                        .map(Template::new);
    }

    public Stream<TemplateGraphs> blendToTemplateGraphs(
                    Template leftTemplate, Template rightTemplate, BlendingOptions blendingOptions) {
        // one way of doing it - probably not the right one...
        //leftTemplate = blendingOperations.replaceBlankNodesWithVariables(leftTemplate);
        //rightTemplate = blendingOperations.replaceBlankNodesWithVariables(rightTemplate);
        RDFDataMgr.write(System.err, new TemplateIO().toDatasetGraph(Set.of(leftTemplate)), Lang.TRIG);
        RDFDataMgr.write(System.err, new TemplateIO().toDatasetGraph(Set.of(rightTemplate)), Lang.TRIG);

        return blendingAlgorithm.blend(new BlendingInstance(blendingOperations, blendingBackground, leftTemplate,
                        rightTemplate, blendingOptions));
    }


}
