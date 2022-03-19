package won.utils.blend.support.shacl;

import org.apache.jena.graph.*;
import org.apache.jena.riot.other.G;

import java.util.*;

import static won.utils.blend.support.shacl.ShapeUtils.extractShapeGraphs;

public class ShaclShapeVariableExtractor {
    private final Graph shapesGraph;
    private final Set<Node> variables;
    private final Map<Node, Graph> shapeGraphs;
    private final Map<Node, Set<Node>> variablesToShapesMap = new HashMap<>();
    private final Map<Node, Set<Node>> shapesToVariablesMap = new HashMap<>();

    public ShaclShapeVariableExtractor(Graph shapesGraph, Set<Node> variables) {
        this.shapesGraph = shapesGraph;
        this.variables = variables;
        this.shapeGraphs = extractShapeGraphs(shapesGraph);
        generateShapeVariableMaps();
    }

    public Map<Node, Graph> getShapeGraphs() {
        return Collections.unmodifiableMap(shapeGraphs);
    }

    public Map<Node, Set<Node>> getVariablesToShapesMap() {
        return Collections.unmodifiableMap(variablesToShapesMap);
    }

    public Map<Node, Set<Node>> getShapesToVariablesMap() {
        return Collections.unmodifiableMap(shapesToVariablesMap);
    }



    private void generateShapeVariableMaps() {
        for (Node variableNode : variables) {
            Set<Node> shapesForVariable = new HashSet<>();
            for (Map.Entry<Node, Graph> entry : shapeGraphs.entrySet()) {
                Graph shapeGraph = entry.getValue();
                Node shapeNode = entry.getKey();
                if (graphContainsVariable(shapeGraph, variableNode)) {
                    shapesForVariable.add(shapeNode);
                    shapesToVariablesMap.compute(shapeNode, (key, vars) -> {
                        if (vars == null) {
                           vars = new HashSet<>();
                           vars.add(variableNode);
                        } else {
                            vars.add(variableNode);
                        }
                        return vars;
                    });
                }
            }
            variablesToShapesMap.put(variableNode, shapesForVariable);
        }
    }

    private static boolean graphContainsVariable(Graph shapeGraph, Node variableNode) {
        return G.contains(shapeGraph, null, null, variableNode) ||
                        G.contains(shapeGraph, variableNode, null, null);
    }



}
