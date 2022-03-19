package won.utils.blend.algorithm.support;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.other.G;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import won.utils.blend.BLEND;
import won.utils.blend.Template;
import won.utils.blend.support.bindings.TemplateBindings;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.graph.BlendedGraphs;
import won.utils.blend.support.graph.TemplateGraphs;
import won.utils.blend.support.io.TemplateIO;

import java.util.*;

public class BlendingOperations {
    private TemplateIO templateIO;

    public BlendingOperations(TemplateIO templateIO) {
        this.templateIO = templateIO;
    }

    public TemplateGraphs blendWithGivenBindings(TemplateGraphs left, TemplateGraphs right,
                    TemplateBindings bindings) {
        Graph data = blendGraphsWithGivenBindings(left.getDataGraph(), right.getDataGraph(), bindings);
        Graph blendingConfig = copyMergeGraphs(left.getBlendingConfigGraph(), right.getBlendingConfigGraph());
        Graph fixedBindings = copyMergeGraphs(left.getBindingsGraph(), right.getBindingsGraph());
        for (VariableBinding binding : bindings.getBindings().getBindingsAsSet()) {
            fixedBindings.add(new Triple(binding.getVariable(), BLEND.boundTo, binding.getBoundNode()));
        }
        Graph shapes = copyMergeGraphs(left.getShapesGraph(), right.getShapesGraph());
        return templateIO.fromGraphs(data, blendingConfig, fixedBindings, shapes);
    }

    public Template replaceBlankNodesWithVariables(Template template){
        Graph targetDataGraph = GraphFactory.createGraphMem();
        targetDataGraph.getPrefixMapping().setNsPrefixes(template.getTemplateGraphs().getDataGraph().getPrefixMapping());
        Graph targetConfigGraph = GraphFactory.createGraphMem();
        targetConfigGraph.getPrefixMapping().setNsPrefixes(template.getTemplateGraphs().getBlendingConfigGraph().getPrefixMapping());
        Map<Node, Node> blankNodeToVariable = new HashMap<>();
        int varInd = 0;
        Set<Triple> newConfigTriples = new HashSet<>();
        Set<Triple> newDataTriples = new HashSet<>();
        Graph dataGraph = template.getTemplateGraphs().getDataGraph();
        ExtendedIterator<Triple> it = dataGraph.find();
        while(it.hasNext()) {
            Triple t = it.next();
            Node obj = t.getObject();
            Node subj = t.getSubject();
            Node newSubject = subj;
            Node newObject = obj;
            Node newPredicate = t.getPredicate();
            if (subj.isBlank()) {
                newSubject = blankNodeToVariable.get(subj);
                if (newSubject == null) {
                    newSubject = templateIO.createURNUUID();
                    blankNodeToVariable.put(subj, newSubject);
                    targetConfigGraph.add(new Triple(newSubject, RDF.type.asNode(), BLEND.Variable));
                    targetConfigGraph.add(new Triple(newSubject, RDF.type.asNode(), BLEND.BlankNodeVariable));
                }
            }
            if (obj.isBlank()){
                newObject = blankNodeToVariable.get(obj);
                if (newObject == null) {
                    newObject = templateIO.createURNUUID();
                    blankNodeToVariable.put(obj, newObject);
                    targetConfigGraph.add(new Triple(newObject, RDF.type.asNode(), BLEND.Variable));
                    targetConfigGraph.add(new Triple(newObject, RDF.type.asNode(), BLEND.BlankNodeVariable));
                }
            }
            if (obj != newObject || subj != newSubject) {
                it.remove();
                targetDataGraph.add(new Triple(newSubject, newPredicate, newObject));
            } else {
                targetDataGraph.add(t);
            }
        }
        template.getTemplateGraphs().getBlendingConfigGraph().stream().forEach(targetConfigGraph::add);
        return new Template(template.getTemplateGraphs().replaceDataGraph(targetDataGraph).replaceBlendingConfigGraph(targetConfigGraph));
    }

    private Graph copyMergeGraphs(Graph left, Graph right) {
        Graph shapes = GraphFactory.createGraphMem();
        if (left != null) {
            G.copyGraphSrcToDst(left, shapes);
            shapes.getPrefixMapping().setNsPrefixes(left.getPrefixMapping());
        }
        if (right != null) {
            G.copyGraphSrcToDst(right, shapes);
            right
                            .getPrefixMapping()
                            .getNsPrefixMap()
                            .entrySet()
                            .forEach(m -> shapes.getPrefixMapping()
                                            .setNsPrefix(m.getKey(), m.getValue()));
        }
        return shapes;
    }

    private Graph blendGraphsWithGivenBindings(Graph leftGraph, Graph rightGraph,
                    TemplateBindings bindings) {
        Graph data = new BlendedGraphs(leftGraph, rightGraph, bindings.getBindings(), false);
        PrefixMapping prefixMapping = data.getPrefixMapping();
        if (leftGraph != null) {
            prefixMapping.setNsPrefixes(leftGraph.getPrefixMapping());
        }
        if (rightGraph != null) {
            rightGraph.getPrefixMapping()
                            .getNsPrefixMap()
                            .entrySet()
                            .forEach(pm -> prefixMapping.setNsPrefix(pm.getKey(), pm.getValue()));
        }
        return data;
    }

    public static Optional<Triple> blendTripleWithGivenBindings(Triple triple, TemplateBindings bindings) {
        if (bindings.hasConstantBindingForVariableAllowTransitive(triple.getSubject())) {
            if (triple.predicateMatches(BLEND.name)) {
                return Optional.of(triple);
            }
            if (triple.predicateMatches(RDF.type.asNode()) && triple.objectMatches(BLEND.Variable)) {
                return Optional.of(triple);
            }
        }
        return Optional.of(
                        new Triple(
                                        mergeNodeWithGivenBindings(triple.getSubject(), bindings),
                                        triple.getPredicate(),
                                        mergeNodeWithGivenBindings(triple.getObject(), bindings)));
    }

    private static Node mergeNodeWithGivenBindings(Node node, TemplateBindings bindings) {
        Optional<Node> result = bindings.getBoundNodeForVariableAllowTransitive(node);
        if (result.isPresent()) {
            return result.get();
        }
        return node;
    }

}
