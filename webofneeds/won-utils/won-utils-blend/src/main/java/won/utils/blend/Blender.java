package won.utils.blend;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

public class Blender {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Graph globalShapesGraph;
    private Graph globalDataGraph;
    private Shapes globalShapes;

    public Blender() {
    }

    public Blender(Graph globalShapesGraph, Graph globalDataGraph) {
        this.globalShapesGraph = globalShapesGraph;
        this.globalDataGraph = globalDataGraph;
        if (globalShapesGraph != null) {
            this.globalShapes = Shapes.parse(globalShapesGraph);
        }
    }

    public Set<Blendable> blend(Blendable left, Blendable right) {
        Set<BlenderBindings> accumulatedBindings = new HashSet<>();
        accumulatedBindings.add(new BlenderBindings());
        Set<Pair<Node, Node>> admissibleBindings = findAdmissibleBindings(left, right);
        admissibleBindings.addAll(findAdmissibleBindings(right, left));
        Set<BlenderBindings> bindings = generateBindingPowerset(admissibleBindings);
        return generateAllBlendingResults(left, right, bindings);
    }

    private Set<Pair<Node, Node>> findAdmissibleBindings(Blendable first,
                    Blendable second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Set<Pair<Node, Node>> resultingCombinations = new HashSet<>();
        Set<Node> variables = findVariables(first);
        Set<Node> constants = findConstants(second);
        for (Node variable : variables) {
            for (Node constant : constants) {
                logger.debug("checking admissiblilty of binding variable {} to  constant {}", variable, constant);
                BlenderBindings bindingsToCheck = new BlenderBindings();
                bindingsToCheck.addBinding(variable, constant);
                Blendable result = blend(first, second, bindingsToCheck);
                if (conformsToBlendableShapes(result)) {
                    resultingCombinations.add(Pair.of(variable, constant));
                    logger.debug("admissible: binding variable {} to  constant {}", variable, constant);
                } else {
                    logger.debug("not admissible: binding variable {} to  constant {}", variable, constant);
                }
            }
        }
        return resultingCombinations;
    }

    private Set<BlenderBindings> generateBindingPowerset(Set<Pair<Node, Node>> admissibleBindings) {
        if (logger.isDebugEnabled()) {
            logger.debug("admissible: {}", Arrays.toString(admissibleBindings.toArray()));
        }
        Set<BlenderBindings> resultingBindings = new HashSet<>();
        resultingBindings.add(new BlenderBindings());
        for (Pair<Node, Node> admissibleBinding : admissibleBindings) {
            Set<BlenderBindings> toAdd = new HashSet<>();
            logger.debug("adding binding of variable {} to constant {}", admissibleBinding.getLeft(),
                            admissibleBinding.getRight());
            for (BlenderBindings bindings : resultingBindings) {
                if (!bindings.hasBindingForVariable(admissibleBinding.getLeft())) {
                    BlenderBindings bindingsWithCurrentVar = (BlenderBindings) bindings.clone();
                    bindingsWithCurrentVar.addBinding(admissibleBinding.getLeft(), admissibleBinding.getRight());
                    toAdd.add(bindingsWithCurrentVar);
                }
            }
            resultingBindings.addAll(toAdd);
        }
        return resultingBindings;
    }

    private Set<Node> findVariables(Blendable blendable) {
        return findByType(blendable.dataGraph, BLEND.Variable);
    }

    private Set<Node> findConstants(Blendable blendable) {
        Set<Node> result = allNodes(blendable.dataGraph);
        result.removeAll(findVariables(blendable));
        result.removeAll(findByType(blendable.dataGraph, BLEND.BlendingDisabled));
        return result;
    }

    private Set<Node> findByType(Graph data, Node type) {
        Set<Node> nodes = new HashSet<>();
        ExtendedIterator<Triple> it = data.find(null, RDF.type.asNode(), type);
        while (it.hasNext()) {
            nodes.add(it.next().getSubject());
        }
        return nodes;
    }

    private Set<Node> allNodes(Graph data) {
        Set<Node> nodes = new HashSet<>();
        ExtendedIterator<Triple> it = data.find();
        while (it.hasNext()) {
            Triple t = it.next();
            nodes.add(t.getSubject());
            nodes.add(t.getObject());
        }
        return nodes;
    }

    private Set<Blendable> generateAllBlendingResults(Blendable left, Blendable right,
                    Set<BlenderBindings> bindingsPowerSet) {
        if (logger.isDebugEnabled()) {
            logger.debug("all bindings: {}", bindingsPowerSet);
        }
        Set<Blendable> results = new HashSet<>();
        for (BlenderBindings bindings : bindingsPowerSet) {
            Blendable result = blend(left, right, bindings);
            if (conformsToBlendableShapes(result)) {
                results.add(result);
            }
        }
        return results;
    }

    private boolean conformsToShapes(Blendable result) {
        Graph actualShapesGraph = combineGraphsIfPresent(result.shapesGraph, this.globalShapesGraph);
        if (actualShapesGraph == null) {
            return true;
        }
        Graph actualDataGraph = combineGraphsIfPresent(result.dataGraph, this.globalDataGraph);
        if (actualDataGraph == null) {
            return true;
        }
        Shapes actualShapes = Shapes.parse(actualShapesGraph);
        ValidationReport report = ShaclValidator.get().validate(actualShapes, actualDataGraph);
        return report.conforms();
    }

    private Graph combineGraphsIfPresent(Graph left, Graph right) {
        Graph result;
        if (left == null) {
            result = right;
        } else if (right == null) {
            result = left;
        } else {
            result = new Union(left, right);
        }
        return result;
    }

    private boolean conformsToBlendableShapes(Blendable result) {
        Shapes blendableShapes = Shapes.parse(result.shapesGraph);
        ValidationReport report = ShaclValidator.get().validate(blendableShapes, result.dataGraph);
        return report.conforms();
    }

    private Blendable blend(Blendable left, Blendable right, BlenderBindings bindings) {
        Graph data = blendGraphs(left.dataGraph, right.dataGraph, bindings);
        Graph shapes = blendGraphs(left.shapesGraph, right.shapesGraph, bindings);
        activateVariableShapes(shapes, bindings);
        return new Blendable(data, shapes);
    }

    private void activateVariableShapes(Graph shapes, BlenderBindings bindings) {
        for (Node boundValue : bindings.bindings.values()) {
            ExtendedIterator<Triple> it = shapes.find(null, SHACL.targetNode, boundValue);
            while (it.hasNext()) {
                Triple t = it.next();
                ExtendedIterator<Triple> it2 = shapes.find(
                                t.getSubject(),
                                SHACL.deactivated, null);
                while (it2.hasNext()) {
                    it2.next();
                    it2.remove();
                }
            }
        }
    }

    private Graph blendGraphs(Graph leftGraph, Graph rightGraph,
                    BlenderBindings bindings) {
        Graph data = GraphFactory.createJenaDefaultGraph();
        if (leftGraph != null) {
            ExtendedIterator<Triple> it = leftGraph.find();
            while (it.hasNext()) {
                blendTriple(it.next(), bindings).ifPresent(data::add);
            }
        }
        if (rightGraph != null) {
            ExtendedIterator<Triple> it = rightGraph.find();
            while (it.hasNext()) {
                blendTriple(it.next(), bindings).ifPresent(data::add);
            }
        }
        return data;
    }

    private Optional<Triple> blendTriple(Triple triple, BlenderBindings bindings) {
        if (bindings.bindings.containsKey(triple.getSubject())) {
            if (triple.predicateMatches(BLEND.name)) {
                return Optional.empty();
            }
            if (triple.predicateMatches(RDF.type.asNode()) && triple.objectMatches(BLEND.Variable)) {
                return Optional.empty();
            }
        }
        return Optional.of(
                        new Triple(
                                        mergeNode(triple.getSubject(), bindings),
                                        triple.getPredicate(),
                                        mergeNode(triple.getObject(), bindings)));
    }

    private Node mergeNode(Node node, BlenderBindings bindings) {
        Node result = bindings.bindings.get(node);
        if (result != null) {
            return result;
        }
        return node;
    }
}
