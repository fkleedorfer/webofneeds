package won.utils.blend.algorithm.join;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.other.G;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.TargetType;
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
import won.utils.blend.support.graph.VariableAwareBlendedGraphs;
import won.utils.blend.support.graph.VariableAwareGraph;
import won.utils.blend.support.graph.VariableAwareGraphImpl;
import won.utils.blend.support.shacl.ValidationReportUtils;
import won.utils.blend.support.shacl.validationlistener.VariableAwareValidationListener;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

public abstract class ShaclValidator {
    public static Set<BindingValidationResult> validateShape(BlendingInstance instance, Shape shape,
                    VariableBindings bindings, AlgorithmState state) {
        VariableAwareGraph data = blendDataGraphs(instance, bindings);
        Set<BindingValidationResult> results = validateShapeOnData(
                        instance, state, data, shape, bindings);
        return results;
    }

    public static VariableAwareGraph blendDataGraphs(BlendingInstance instance, VariableBindings bindings) {
        VariableAwareGraph data = new VariableAwareBlendedGraphs(
                        instance.blendingBackground.combineWithBackgroundData(instance.leftTemplate.getTemplateGraphs()
                                                        .getDataGraph()),
                                                        instance.rightTemplate.getTemplateGraphs().getDataGraph(),
                                                        bindings);
        return data;
    }

    public static Set<Shape> getShapesTargetedOnBindings(AlgorithmState state, VariableAwareGraphImpl data,
                    VariableBindings bindings) {
        Set<Node> mainNodes = new HashSet<>(bindings.getDecidedVariables());
        state.log.finerTrace(() -> "main nodes: " + mainNodes);
        mainNodes.addAll(bindings.getBindingsAsSet().stream().map(VariableBinding::getBoundNode).collect(toSet()));
        Set<Shape> impliedShapes = ShaclValidator.getShapesWithTargetNodes(state, data, mainNodes);
        state.log.finerTrace(() -> "implied shapes: " + impliedShapes.stream().map(s -> s.getShapeNode().toString())
                        .collect(Collectors.joining(", ")));
        return impliedShapes;
    }

    public static Set<BindingValidationResult> validateShapeOnData(BlendingInstance instance, AlgorithmState state,
                    VariableAwareGraph data, Shape shape,
                    VariableBindings bindings) {
        state.log.finerTrace(() -> "validating shape " + shape.getShapeNode() + " with bindings " + bindings
                        .getBindingsAsSet());
        Collection<Node> focusNodes = VLib.focusNodes(data, shape);
        state.log.finerTrace(() -> "found " + focusNodes.size() + " focus nodes");
        data.resetEncounteredVariables();
        return validateShapeForFocusNodesOnData(instance, state, data, bindings, shape, focusNodes.stream().collect(toSet()));
    }

    public static Set<BindingValidationResult> validateShapeForFocusNodesOnData(BlendingInstance instance,
                    AlgorithmState state, VariableAwareGraph data, VariableBindings bindings, Shape shape,
                    Set<Node> focusNodes) {
        Objects.requireNonNull(focusNodes);
        Objects.requireNonNull(bindings);
        Objects.requireNonNull(data);
        Objects.requireNonNull(shape);
        Objects.requireNonNull(state);
        Objects.requireNonNull(instance);
        return state.log.logIndented(() -> {
            Set<BindingValidationResult> results = new HashSet<>();
            state.log.finerTrace(() -> "validating " + focusNodes.size() + " focus nodes");

            for (Node node : focusNodes) {
                BindingValidationResult result = validateFocusNodeOnData(instance, state, data, shape, bindings, node);
                if (result != null) {
                    results.add(result);
                }
            }
            if (focusNodes.isEmpty()) {
                return Set.of(validateShapeWithoutFocusNode(instance, state, shape, bindings));
            }
            return results;
        });
    }

    public static BindingValidationResult validateFocusNode(BlendingInstance instance, AlgorithmState state,
                    Shape shape,
                    Node focusNode, VariableBindings bindings) {
        VariableAwareGraph data = blendDataGraphs(instance, bindings);
        return validateFocusNodeOnData(instance, state, data, shape, bindings, focusNode);
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
                    AlgorithmState state, VariableAwareGraph data, Shape shape,
                    VariableBindings bindings, Node focusNode) {
        return state.log.logIndented(() -> {
            ShaclErrorHandler errorHandler = new ShaclErrorHandler();
            VariableAwareValidationListener validationListener = new VariableAwareValidationListener(data);
            ValidationContext shaclValidationContext = ValidationContext.create(state.shapes, data, errorHandler, validationListener);
            Node effectiveFocusNode = determineFocusNode(shape, bindings, focusNode);
            state.log.finerTrace(() -> "Checking focus node " + effectiveFocusNode);
            data.resetEncounteredVariables();
            VLib.validateShape(shaclValidationContext, data, shape, effectiveFocusNode);
            state.log.debug(() -> "evaluations: \n" + validationListener.getEvaluations().stream().map(e -> e.toPrettyString()).collect(Collectors.joining("\n")));
            Set<Node> encounteredVariables = data.getEncounteredVariables();
            encounteredVariables.remove(focusNode);
            BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
            encounteredVariables.addAll(instanceLogic.getVariablesUsedInShape(shape.getShapeNode()));
            findNamedShapesForErrorsInReport(shaclValidationContext.generateReport(), state.shapes)
                            .stream()
                            .flatMap(s -> instanceLogic.getVariablesUsedInShape(s).stream())
                            .forEach(encounteredVariables::add);
            boolean encounteredAVariableBoundToAVariableOrUnbound = encounteredVariables.stream()
                            .anyMatch(e -> {
                                if (e.isBlank()) {
                                    // for a blank node var, we want to ignore errors as long as the var is
                                    // undecided
                                    return !bindings.isAlreadyDecidedVariable(e);
                                }
                                Optional<Node> dereferenced = bindings.dereferenceIfVariable(e);
                                if (dereferenced.isEmpty()) {
                                    return true;
                                }
                                return bindings.isNonBlankVariable(dereferenced.get());
                            });
            if (bindings.getVariables().contains(focusNode)) {
                encounteredVariables.add(focusNode);
            }
            encounteredVariables.removeAll(bindings.getDecidedVariables());
            encounteredVariables.removeAll(bindings.getBoundNodes());
            if (encounteredVariables.isEmpty()) {
                if (encounteredAVariableBoundToAVariableOrUnbound) {
                    if (state.log.isDebugEnabled()) {
                        if (shaclValidationContext.hasViolation()) {
                            state.log.debugFmt(
                                            "shape %s is invalid but we ignore this because an encountered var is unbound or bound to a var",
                                            shape);
                            if (state.log.isFinerTraceEnabled()) {
                                state.log.logIndented(() -> {
                                    state.log.finerTrace(() -> "validation report:");
                                    state.log.finerTrace(() -> ValidationReportUtils.toString(shaclValidationContext.generateReport()));
                                });
                            }
                        }
                    }
                    state.log.finerTrace(
                                    () -> "focus node may or may not be invalid, an encountered var is unbound or bound to a var, so we cannot fail the node because of this");
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
            state.log.finerTrace(
                            () -> "encountered new variables: " + encounteredVariables.stream().map(Object::toString)
                                            .collect(joining(", ")));
            return new BindingValidationResult(shape, focusNode, bindings, encounteredVariables, Ternary.UNKNOWN);
        });
    }

    private static Node determineFocusNode(Shape shape, VariableBindings bindings, Node focusNode) {
        if (isExplicitTargetOfShape(shape, focusNode)){
            return focusNode;
        }
        return  bindings.dereferenceIfVariable(focusNode).orElse(focusNode);
    }

    private static boolean isExplicitTargetOfShape(Shape shape, Node focusNode) {
        return shape.getTargets().stream()
                        .anyMatch(t -> t.getTargetType() == TargetType.targetNode && t.getObject().equals(
                                        focusNode));
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
        Set<Node> incomingFrom = G
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
