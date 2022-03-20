package won.utils.blend.support.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.other.G;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.graph.GraphFactory;

import java.util.UUID;

public class TemplateGraphs {
    private final NamedGraph blendingConfigNamedGraph;
    private final NamedGraph dataNamedGraph;
    private final NamedGraph shapesNamedGraph;
    private final NamedGraph mainTemplateNamedGraph;
    private final NamedGraph bindingsNamedGraph;

    public TemplateGraphs(NamedGraph mainTemplate,
                    NamedGraph data,
                    NamedGraph blendingConfig,
                    NamedGraph bindings,
                    NamedGraph shapes) {
        this.dataNamedGraph = data;
        this.shapesNamedGraph = shapes;
        this.mainTemplateNamedGraph = mainTemplate;
        this.blendingConfigNamedGraph = blendingConfig;
        this.bindingsNamedGraph = bindings;
    }

    public TemplateGraphs replaceDataGraph(Graph newGraph) {
        NamedGraph ng = null;
        if (this.dataNamedGraph == null) {
            ng = new NamedGraph(NodeFactory.createURI("urn:uuid:" + UUID.randomUUID().toString()), newGraph);
        } else {
            ng = new NamedGraph(this.dataNamedGraph.node, newGraph);
        }
        return new TemplateGraphs(mainTemplateNamedGraph, ng, blendingConfigNamedGraph, bindingsNamedGraph,
                        shapesNamedGraph);
    }

    public TemplateGraphs replaceBlendingConfigGraph(Graph newGraph) {
        NamedGraph ng = null;
        if (this.blendingConfigNamedGraph == null) {
            ng = new NamedGraph(NodeFactory.createURI("urn:uuid:" + UUID.randomUUID().toString()), newGraph);
        } else {
            ng = new NamedGraph(this.blendingConfigNamedGraph.node, newGraph);
        }
        return new TemplateGraphs(mainTemplateNamedGraph, dataNamedGraph, ng, bindingsNamedGraph, shapesNamedGraph);
    }

    public Node getTemplateNode() {
        return mainTemplateNamedGraph.node;
    }

    public Graph getDataGraph() {
        return this.dataNamedGraph == null ? Graph.emptyGraph : this.dataNamedGraph.graph;
    }

    public Graph getBlendingConfigGraph() {
        return this.blendingConfigNamedGraph == null ? Graph.emptyGraph : blendingConfigNamedGraph.graph;
    }

    public Graph getBindingsGraph() {
        return this.bindingsNamedGraph == null ? Graph.emptyGraph : bindingsNamedGraph.graph;
    }

    public Graph getShapesGraph() {
        return this.shapesNamedGraph == null ? Graph.emptyGraph : shapesNamedGraph.graph;
    }

    public boolean hasBlendingConfig() {
        return this.blendingConfigNamedGraph != null;
    }

    public boolean hasBindings() {
        return this.bindingsNamedGraph != null;
    }

    public boolean hasShapes() {
        return this.shapesNamedGraph != null;
    }

    public void copyToDatasetGraph(DatasetGraph datasetGraph) {
        datasetGraph.addGraph(mainTemplateNamedGraph.node, copyGraph(mainTemplateNamedGraph.graph));
        if (dataNamedGraph != null) {
            copyToResult(datasetGraph, dataNamedGraph);
        }
        if (blendingConfigNamedGraph != null) {
            copyToResult(datasetGraph, blendingConfigNamedGraph);
        }
        if (bindingsNamedGraph != null) {
            copyToResult(datasetGraph, bindingsNamedGraph);
        }
        if (shapesNamedGraph != null) {
            copyToResult(datasetGraph, shapesNamedGraph);
        }
    }

    private void copyToResult(DatasetGraph datasetGraph, NamedGraph dataNamedGraph) {
        Graph copy = copyGraph(dataNamedGraph.graph);
        datasetGraph.addGraph(dataNamedGraph.node, copy);
        copy.getPrefixMapping().getNsPrefixMap().entrySet().stream().forEach(e -> datasetGraph.getDefaultGraph()
                        .getPrefixMapping().setNsPrefix(e.getKey(), e.getValue()));
    }

    public Graph copyGraph(Graph graph) {
        Graph copy = GraphFactory.createGraphMem();
        G.copyGraphSrcToDst(graph, copy);
        copy.getPrefixMapping().setNsPrefixes(graph.getPrefixMapping());
        return copy;
    }
}