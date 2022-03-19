package won.utils.blend.support.io;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.utils.blend.BLEND;
import won.utils.blend.Template;
import won.utils.blend.support.graph.NamedGraph;
import won.utils.blend.support.graph.TemplateGraphs;
import won.utils.blend.support.uuid.RandomUUIDSource;
import won.utils.blend.support.uuid.UUIDSource;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TemplateIO {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private UUIDSource uuidSource;

    public TemplateIO(UUIDSource uuidSource) {
        this.uuidSource = uuidSource;
    }

    public TemplateIO() {
        this(new RandomUUIDSource());
    }

    public Set<Template> fromDatasetGraph(DatasetGraph datasetGraph) {
        Set<Template> result = new HashSet<>();
        Set<Node> templateNodes = findTemplates(datasetGraph);
        for (Node templateNode : templateNodes) {
            Template template = makeTemplate(datasetGraph, templateNode);
            result.add(template);
        }
        return result;
    }

    public TemplateGraphs fromGraphs(Graph dataGraph, Graph blendingConfigGraph, Graph bindingsGraph,
                    Graph shapesGraph) {
        Graph templateMain = GraphFactory.createGraphMem();
        Node templateMainNode = createURNUUID();
        Node blendingConfigNode = createURNUUID();
        Node bindingsNode = createURNUUID();
        Node shapesNode = createURNUUID();
        templateMain.add(new Triple(templateMainNode, RDF.type.asNode(), BLEND.Template));
        Node dataNode = createURNUUID();
        templateMain.add(new Triple(templateMainNode, BLEND.dataGraph, dataNode));
        if (shapesGraph != null) {
            templateMain.add(new Triple(templateMainNode, BLEND.shapesGraph, shapesNode));
        }
        if (blendingConfigGraph != null) {
            templateMain.add(new Triple(templateMainNode, BLEND.blendingConfigGraph, blendingConfigNode));
        }
        if (bindingsGraph != null) {
            templateMain.add(new Triple(templateMainNode, BLEND.bindingsGraph, bindingsNode));
        }
        return new TemplateGraphs(
                        new NamedGraph(templateMainNode, templateMain),
                        new NamedGraph(dataNode, dataGraph),
                        blendingConfigGraph == null ? null : new NamedGraph(blendingConfigNode, blendingConfigGraph),
                        bindingsGraph == null ? null : new NamedGraph(bindingsNode, bindingsGraph),
                        shapesGraph == null ? null : new NamedGraph(shapesNode, shapesGraph));
    }

    public Node createURNUUID() {
        return NodeFactory.createURI("urn:" + uuidSource.createUUID());
    }

    public DatasetGraph toDatasetGraph(Set<Template> templates) {
        return templateGraphsToDatasetGraph(
                        templates.stream().map(Template::getTemplateGraphs).collect(Collectors.toSet()));
    }

    public void toDatasetGraph(DatasetGraph datasetGraph, Set<Template> templates) {
        templates.stream().forEach(t -> templateGraphsToDatasetGraph(datasetGraph, t.getTemplateGraphs()));
    }

    public void toDatasetGraph(DatasetGraph datasetGraph, Template template) {
        templateGraphsToDatasetGraph(datasetGraph, template.getTemplateGraphs());
    }

    public DatasetGraph templateGraphsToDatasetGraph(Set<TemplateGraphs> templateGraphs) {
        DatasetGraph datasetGraph = DatasetGraphFactory.createGeneral();
        templateGraphsToDatasetGraph(datasetGraph, templateGraphs);
        return datasetGraph;
    }

    public void templateGraphsToDatasetGraph(DatasetGraph datasetGraph, Set<TemplateGraphs> templateGraphs) {
        templateGraphs.stream().forEach(t -> templateGraphsToDatasetGraph(datasetGraph, t));
    }

    public void templateGraphsToDatasetGraph(DatasetGraph datasetGraph, TemplateGraphs templateGraphs) {
        templateGraphs.copyToDatasetGraph(datasetGraph);
    }

    private Template makeTemplate(DatasetGraph datasetGraph, Node template) {
        Optional<Node> dataGraphNode = obtainDataGraphNode(datasetGraph, template);
        NamedGraph dataGraph = dataGraphNode
                        .map(n -> getGraphWithPrefixes(datasetGraph, n).orElse(null))
                        .map(g -> new NamedGraph(dataGraphNode.get(), g))
                        .orElse(null);
        Optional<Node> blendingConfigGraphNode = obtainBlendingConfigGraphNode(datasetGraph, template);
        NamedGraph blendingConfigGraph = blendingConfigGraphNode
                        .map(n -> getGraphWithPrefixes(datasetGraph, n).orElse(null))
                        .map(g -> new NamedGraph(blendingConfigGraphNode.get(), g))
                        .orElse(null);
        Optional<Node> bindingsGraphNode = obtainBindingsGraphNode(datasetGraph, template);
        NamedGraph bindingsGraph = bindingsGraphNode
                        .map(n -> getGraphWithPrefixes(datasetGraph, n).orElse(null))
                        .map(g -> new NamedGraph(bindingsGraphNode.get(), g))
                        .orElse(null);
        Optional<Node> shapesGraphNode = obtainShapesGraphNode(datasetGraph, template);
        NamedGraph shapesGraph = shapesGraphNode
                        .map(n -> getGraphWithPrefixes(datasetGraph, n).orElse(null))
                        .map(g -> new NamedGraph(shapesGraphNode.get(), g))
                        .orElse(null);
        NamedGraph templateMainGraph = new NamedGraph(template, getGraphWithPrefixes(datasetGraph, template).get());
        TemplateGraphs result = new TemplateGraphs(templateMainGraph, dataGraph, blendingConfigGraph, bindingsGraph,
                        shapesGraph);
        return new Template(result);
    }

    private Optional<Node> obtainDataGraphNode(DatasetGraph datasetGraph, Node template) {
        Set<Node> nodes = findByPredicate(datasetGraph, template, BLEND.dataGraph);
        if (nodes.size() > 1) {
            throw new IllegalStateException(String.format("Template %s has more than one dataGraph", template));
        }
        if (nodes.isEmpty()) {
            throw new IllegalStateException(String.format("Template %s does not have a dataGraph", template));
        }
        return nodes.stream().findFirst();
    }

    private Optional<Node> obtainBlendingConfigGraphNode(DatasetGraph datasetGraph, Node template) {
        Set<Node> nodes = findByPredicate(datasetGraph, template, BLEND.blendingConfigGraph);
        if (nodes.size() > 1) {
            throw new IllegalStateException(
                            String.format("Template %s has more than one blendingConfigGraph", template));
        }
        if (nodes.isEmpty()) {
            logger.debug("Template {} does not have a blendingConfigGraph", template);
            return Optional.empty();
        }
        return nodes.stream().findFirst();
    }

    private Optional<Node> obtainBindingsGraphNode(DatasetGraph datasetGraph, Node template) {
        Set<Node> nodes = findByPredicate(datasetGraph, template, BLEND.bindingsGraph);
        if (nodes.size() > 1) {
            throw new IllegalStateException(String.format("Template %s has more than one bindingsGraph", template));
        }
        if (nodes.isEmpty()) {
            logger.debug("Template {} does not have a bindingsGraph", template);
            return Optional.empty();
        }
        return nodes.stream().findFirst();
    }

    private Optional<Node> obtainShapesGraphNode(DatasetGraph datasetGraph, Node template) {
        Set<Node> nodes = findByPredicate(datasetGraph, template, BLEND.shapesGraph);
        if (nodes.size() > 1) {
            throw new IllegalStateException(String.format("Template %s has more than one shapesGraph", template));
        }
        if (nodes.isEmpty()) {
            logger.debug("Template {} does not have a shapesGraph", template);
            return Optional.empty();
        }
        return nodes.stream().findFirst();
    }

    private Optional<Graph> getGraphWithPrefixes(DatasetGraph datasetGraph, Node graphNode) {
        if (graphNode == null) {
            return Optional.empty();
        }
        Graph graph = datasetGraph.getGraph(graphNode);
        if (graph != null) {
            graph.getPrefixMapping().setNsPrefixes(datasetGraph.getDefaultGraph().getPrefixMapping());
        }
        return Optional.ofNullable(graph);
    }

    private Set<Node> findTemplates(DatasetGraph datasetGraph) {
        Set<Node> nodes = new HashSet<>();
        Iterator<Quad> it = datasetGraph.find(null, null, RDF.type.asNode(), BLEND.Template);
        while (it.hasNext()) {
            nodes.add(it.next().getSubject());
        }
        return nodes;
    }

    private Set<Node> findByPredicate(DatasetGraph datasetGraph, Node subject, Node predicate) {
        Set<Node> nodes = new HashSet<>();
        Iterator<Quad> it = datasetGraph.find(null, subject, predicate, null);
        while (it.hasNext()) {
            nodes.add(it.next().getObject());
        }
        return nodes;
    }
}
