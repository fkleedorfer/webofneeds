package won.utils.blend.algorithm.support.instance;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.ValidationContext;
import won.utils.blend.BLEND;
import won.utils.blend.BlendingOptions;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.VLib;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.shacl.ShapeInShapes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

public class BlendingInstanceLogic {
    private BlendingInstance instance;

    public BlendingInstanceLogic(BlendingInstance instance) {
        this.instance = instance;
    }

    public boolean isVariable(Node node) {
        return instance.leftTemplate.isVariable(node) || instance.rightTemplate.isVariable(node);
    }

    public boolean isConstant(Node node) {
        return instance.leftTemplate.isConstant(node) || instance.rightTemplate.isConstant(node);
    }

    /**
     * Returns the options for the specified <code>variable</code> in the instance.
     * Depending on {@link BlendingOptions#getUnboundHandlingMode()},
     * {@link BLEND#unbound} may or may not be an option. If <code>variable</code>
     * is a blank node, {@link BLEND#unbound} and all blank nodes in the other
     * {@link won.utils.blend.Template} are options. If <code>variable</code> is an
     * IRI (and, therefore <code>rdf:type bl:Variable</code>), it may have
     * <code>bl:candidateShape</code>s, in which case all nodes are options that are
     * valid target nodes of those shapes in the other
     * {@link won.utils.blend.Template}.
     * 
     * @param variable
     * @return
     */
    public Set<Node> getBindingOptions(Node variable) {
        if (variable.isBlank()) {
            return getBindingOptionsForBlankNode(variable);
        }
        return getBindingOptionsForVariable(variable);
    }

    private Set<Node> getBindingOptionsForVariable(Node variable) {
        Set<Node> options = new HashSet<>();
        if (!instance.blendingOptions.getUnboundHandlingMode().isAllBound()) {
            options.add(BLEND.unbound);
        }
        Set<ShapeInShapes> candidateShapes = getCandidateShapesForVariable(variable);
        if (candidateShapes.isEmpty()) {
            options.addAll(getUnrestrictedBindingOptions(variable));
        } else {
            options.addAll(getBindingOptionsAsCandidateShapesFocusNodes(variable, candidateShapes));
            options.addAll(getCompatibleVariablesByCandidateShapes(variable, candidateShapes));
        }
        return options;
    }

    private Set<Node> getBindingOptionsForBlankNode(Node variable) {
        Set<Node> options = new HashSet<>();
        options.add(BLEND.unbound);
        if (instance.leftTemplate.isBlankNode(variable)) {
            options.addAll(instance.rightTemplate.getBlankNodes());
        } else if (instance.rightTemplate.isBlankNode(variable)) {
            options.addAll(instance.leftTemplate.getBlankNodes());
        }
        return options;
    }

    private Set<Node> getCompatibleVariablesByCandidateShapes(Node variable, Set<ShapeInShapes> candidateShapes) {
        Set<Node> candidateVariables = null;
        if (instance.leftTemplate.isVariable(variable)) {
            candidateVariables = instance.rightTemplate.getVariables();
        } else if (instance.rightTemplate.isVariable(variable)) {
            candidateVariables = instance.leftTemplate.getVariables();
        } else {
            throw new IllegalArgumentException("Not a variable: " + variable);
        }
        return candidateVariables.stream()
                        .filter(v -> candidateShapes.stream()
                                        .anyMatch(s -> getCandidateShapesForVariable(v).contains(s)))
                        .collect(Collectors.toSet());
    }

    private Set<Node> getBindingOptionsAsCandidateShapesFocusNodes(Node variable, Set<ShapeInShapes> candidateShapes) {
        Set<Node> options = new HashSet<>();
        Graph data = null;
        Graph dataWithBackground = null;
        if (instance.leftTemplate.isVariable(variable)) {
            data = instance.rightTemplate.getTemplateGraphs().getDataGraph();
            dataWithBackground = instance.blendingBackground
                            .combineWithBackgroundData(instance.rightTemplate.getTemplateGraphs().getDataGraph());
        } else if (instance.rightTemplate.isVariable(variable)) {
            data = instance.leftTemplate.getTemplateGraphs().getDataGraph();
            dataWithBackground = instance.blendingBackground
                            .combineWithBackgroundData(instance.leftTemplate.getTemplateGraphs().getDataGraph());
        } else {
            throw new IllegalArgumentException("Not a variable: " + variable);
        }
        for (ShapeInShapes candidateShape : candidateShapes) {
            Collection<Node> focusNodes = VLib.focusNodes(dataWithBackground, candidateShape.getShape());
            Graph finalData = data;
            focusNodes.removeIf(n -> !finalData.contains(n, null, null)
                            && !finalData.contains(null, null, n));
            for (Node focusNode : focusNodes) {
                ValidationContext ctx = ValidationContext.create(candidateShape.getShapes(), dataWithBackground);
                VLib.validateShape(ctx, dataWithBackground, candidateShape.getShape(), focusNode);
                if (!ctx.hasViolation()) {
                    options.add(focusNode);
                }
            }
        }
        return options;
    }

    public Set<Node> getUnrestrictedBindingOptions(Node variable) {
        if (instance.leftTemplate.isVariable(variable)) {
            return Stream.concat(
                            instance.rightTemplate.getConstants().stream(),
                            instance.rightTemplate.getVariables().stream().filter(not(Node::isBlank)))
                            .collect(toSet());
        } else if (instance.rightTemplate.isVariable(variable)) {
            return Stream.concat(
                            instance.leftTemplate.getConstants().stream(),
                            instance.leftTemplate.getVariables().stream().filter(not(Node::isBlank)))
                            .collect(toSet());
        }
        return Collections.emptySet();
    }

    public Shapes getShapes() {
        return instance.shapes;
    }

    public Set<Node> getVariables() {
        return Stream.concat(
                        this.instance.leftTemplate.getVariables().stream(),
                        this.instance.rightTemplate.getVariables().stream())
                        .collect(Collectors.toUnmodifiableSet());
    }

    public Set<ShapeInShapes> getCandidateShapesForVariable(Node variable) {
        return instance.candidateShapesByVariable.getOrDefault(variable, Collections.emptySet());
    }

    public Set<Node> getVariablesUsedInShape(Node shapeNode) {
        return Stream.concat(
                        this.instance.leftTemplate.getVariablesUsedInShape(shapeNode).stream(),
                        this.instance.rightTemplate.getVariablesUsedInShape(shapeNode).stream()).collect(
                                        Collectors.toUnmodifiableSet());
    }

    public VariableBindings getFixedBindings() {
        return new VariableBindings(getVariables(), Stream.concat(
                        this.instance.leftTemplate.getFixedBindings().getBindingsAsSet().stream(),
                        this.instance.rightTemplate.getFixedBindings().getBindingsAsSet().stream())
                        .collect(toSet()));
    }
}
