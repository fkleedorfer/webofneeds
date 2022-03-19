package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.other.G;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.validation.VLib;
import org.apache.jena.shacl.vocabulary.SHACL;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.graph.BlendedGraphs;
import won.utils.blend.support.graph.VariableAwareGraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

public abstract class ShaclValidator {
    public static Set<BindingValidationResult> validateShape(BlendingInstance instance, Shape shape,
                    VariableBindings bindings, AlgorithmState state) {
        VariableAwareGraph data = blendDataGraphs(instance, bindings);
        Set<BindingValidationResult> results = validateShapeOnData(
                        instance, shape, bindings, state, data);
        return results;
    }

    public static VariableAwareGraph blendDataGraphs(BlendingInstance instance, VariableBindings bindings) {
        BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
        VariableAwareGraph data = new VariableAwareGraph(
                        instance.blendingBackground.combineWithBackgroundData(
                                        new BlendedGraphs(instance.leftTemplate.getTemplateGraphs()
                                                        .getDataGraph(),
                                                        instance.rightTemplate.getTemplateGraphs().getDataGraph(),
                                                        bindings)),
                        instanceLogic::isVariable);
        return data;
    }

    public static Set<Shape> getShapesTargetedOnBindings(AlgorithmState state, VariableAwareGraph data,
                    VariableBindings bindings) {
        Set<Node> mainNodes = new HashSet<>(bindings.getDecidedVariables());
        state.log.finerTrace(() -> "main nodes: " + mainNodes);
        mainNodes.addAll(bindings.getBindingsAsSet().stream().map(VariableBinding::getBoundNode).collect(toSet()));
        Set<Shape> impliedShapes = ShaclValidator.getShapesWithTargetNodes(state, data, mainNodes);
        state.log.finerTrace(() -> "implied shapes: " + impliedShapes.stream().map(s -> s.getShapeNode().toString())
                        .collect(Collectors.joining(", ")));
        return impliedShapes;
    }

    public static Set<BindingValidationResult> validateShapeOnData(BlendingInstance instance, Shape shape,
                    VariableBindings bindings, AlgorithmState state, VariableAwareGraph data) {
        state.log.finerTrace(() -> "validating shape " + shape.getShapeNode() + " with bindings " + bindings
                        .getBindingsAsSet());
        Collection<Node> focusNodes = VLib.focusNodes(data, shape);
        data.reset();
        return state.log.logIndented(() -> {
            Set<BindingValidationResult> results = new HashSet<>();
            state.log.finerTrace(() -> "found " + focusNodes.size() + " focus nodes");
            for (Node node : focusNodes) {
                BindingValidationResult result = validateFocusNodeOnData(instance, state, shape, node, bindings, data);
                if (result != null) {
                    results.add(result);
                }
            }
            if (focusNodes.isEmpty()) {
                return Set.of(validateShapeWithoutFocusNode(instance,state, shape, bindings));
            }
            return results;
        });
    }

    public static BindingValidationResult validateFocusNode(BlendingInstance instance, AlgorithmState state,
                    Shape shape,
                    Node focusNode, VariableBindings bindings) {
        VariableAwareGraph data = blendDataGraphs(instance, bindings);
        return validateFocusNodeOnData(instance, state, shape, focusNode, bindings, data);
    }

    public static BindingValidationResult validateShapeWithoutFocusNode(BlendingInstance instance, AlgorithmState state,
                    Shape shape, VariableBindings bindings) {
        Set<Node> encounteredVariables = new HashSet<>();
        BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
        encounteredVariables.addAll(instanceLogic.getVariablesUsedInShape(shape.getShapeNode()));
        encounteredVariables.removeAll(bindings.getDecidedVariables());
        return new BindingValidationResult(shape, null, bindings, encounteredVariables, Ternary.TRUE);
    }

    public static BindingValidationResult validateFocusNodeOnData(BlendingInstance instance,
                    AlgorithmState state, Shape shape,
                    Node focusNode,
                    VariableBindings bindings,
                    VariableAwareGraph data) {

        return state.log.logIndented(() -> {
            ShaclErrorHandler errorHandler = new ShaclErrorHandler();
            ValidationContext shaclValidationContext = ValidationContext.create(state.shapes, data, errorHandler);
            state.log.finerTrace(() -> "Checking focus node " + focusNode);
            data.reset();
            VLib.validateShape(shaclValidationContext, data, shape, focusNode);
            Set<Node> encounteredVariables = data.getEncounteredVariables();
            if (bindings.getVariables().contains(focusNode)){
                encounteredVariables.add(focusNode);
            }
            BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
            encounteredVariables.addAll(instanceLogic.getVariablesUsedInShape(shape.getShapeNode()));
            findNamedShapesForErrorsInReport(shaclValidationContext.generateReport(), state.shapes)
                            .stream()
                            .flatMap(s -> instanceLogic.getVariablesUsedInShape(s).stream())
                            .forEach(encounteredVariables::add);
            boolean encounteredAVariableBoundToAVariableOrUnbound = encounteredVariables.stream().anyMatch(e -> {
                Optional<Node> dereferenced = bindings.dereferenceIfVariable(e);
                if (dereferenced.isEmpty()){
                    return true;
                }
                return bindings.isVariable(dereferenced.get());
            });
            encounteredVariables.removeAll(bindings.getDecidedVariables());
            encounteredVariables.removeAll(bindings.getBoundNodes());
            if (encounteredVariables.isEmpty()) {
                if (encounteredAVariableBoundToAVariableOrUnbound) {
                    state.log.finerTrace(() -> "focus node may or may not be invalid, but we bound a variable to another, so we cannot fail the node because of this");
                    return new BindingValidationResult(shape, focusNode, bindings, encounteredVariables, Ternary.TRUE);
                }
                if (shaclValidationContext.hasViolation()) {
                    state.log.finerTrace(() -> "focus node is invalid - binding cannot lead to acceptable result");
                    return new BindingValidationResult(shape, focusNode, bindings, encounteredVariables, Ternary.FALSE);
                } else {
                    state.log.finerTrace(() -> "focus node is valid, no new variables encountered");
                    return new BindingValidationResult(shape, focusNode, bindings, encounteredVariables, Ternary.TRUE);
                }
            }
            state.log.finerTrace(() ->
                            "encountered new variables: " + encounteredVariables.stream().map(Object::toString)
                                            .collect(joining(", ")));
            return new BindingValidationResult(shape, focusNode, bindings, encounteredVariables, Ternary.UNKNOWN);
        });
    }


    private static Set<Node> findNamedShapesForErrorsInReport(ValidationReport validationReport, Shapes shapes) {
        Set<Node> namedShapes = new HashSet<>();
        for (ReportEntry entry : validationReport.getEntries()) {
            namedShapes.addAll(findNamedShapeForShape(entry.source(), shapes));
        }
        return namedShapes;
    }

    private static Set<Node> findNamedShapeForShape(Node source, Shapes shapes) {
        return findNamedShapeForShape(source, shapes, 0);
    }

    private static Set<Node> findNamedShapeForShape(Node source, Shapes shapes, int depth) {
        if (depth > 50) {
            throw new IllegalStateException(
                            "Max recursion depth exceeded for this operation. Either the shapes are very deep or there is a cycle somewhere");
        }
        Shape sourceShape = shapes.getShape(source);
        if (sourceShape != null) {
            if (shapes.getTargetShapes().contains(sourceShape)) {
                return Set.of(sourceShape.getShapeNode());
            }
        }
        Set<Node> incomingFrom =
                        G
                                        .find(shapes.getGraph(), Node.ANY, Node.ANY, source)
                                        .filterKeep(t -> t
                                                        .getPredicate()
                                                        .toString()
                                                        .startsWith(SHACL.getURI()))
                                        .mapWith(t -> t.getSubject()).toSet();
        return incomingFrom
                        .stream()
                        .flatMap(in -> findNamedShapeForShape(in, shapes, depth + 1).stream())
                        .collect(Collectors.toSet());
    }

    private static String setDiffToString(Set<Node> set, Set<Node> minus) {
        Set<Node> s = new HashSet<>(set);
        s.removeAll(minus);
        return s.stream().map(Object::toString).collect(joining(",", "[", "]"));
    }

    public static Set<Shape> getShapesWithTargetNodes(AlgorithmState state, Graph data, Set<Node> targetNodes) {
        Set<Shape> shapes = new HashSet<>();
        for (Shape targetShape : state.shapes.getTargetShapes()) {
            for (Node node : targetNodes) {
                if (VLib.isFocusNode(targetShape, node, data)) {
                    shapes.add(targetShape);
                }
            }
        }
        return shapes;
    }

    private static class ShaclErrorHandler implements ErrorHandler {
        public ShaclErrorHandler() {
        }

        boolean error = false;
        boolean fatal = false;
        boolean warning = false;

        @Override
        public void warning(String s, long l, long l1) {
            warning = true;
        }

        @Override
        public void error(String s, long l, long l1) {
            warning = true;
        }

        @Override
        public void fatal(String s, long l, long l1) {
            warning = true;
        }

        public boolean isError() {
            return error;
        }

        public boolean isFatal() {
            return fatal;
        }

        public boolean isWarning() {
            return warning;
        }
    }
}
