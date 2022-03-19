package won.utils.blend.algorithm.sat.shacl2;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.other.G;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.vocabulary.SHACL;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.astarish.AlgorithmState;
import won.utils.blend.algorithm.sat.shacl2.astarish.SearchNode;
import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.algorithm.support.bindings.CompactVariables;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.graph.BlendedGraphs;
import won.utils.blend.support.graph.VariableAwareGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
                        instance.blendingBackground.combineWithBackgroundData(new BlendedGraphs(instance.leftTemplate.getTemplateGraphs()
                        .getDataGraph(), instance.rightTemplate.getTemplateGraphs().getDataGraph(),
                        bindings.getBindingsAsSet())),
                        instanceLogic::isVariable);
        return data;
    }

    public static Set<Shape> getShapesTargetedOnBindings(AlgorithmState state, VariableAwareGraph data, VariableBindings bindings) {
        Set<Node> mainNodes = new HashSet<>(bindings.getDecidedVariables());
        state.log.trace(() -> "main nodes: " + mainNodes);
        mainNodes.addAll(bindings.getBindingsAsSet().stream().map(VariableBinding::getBoundNode).collect(toSet()));
        Set<Shape> impliedShapes = ShaclValidator.getShapesWithTargetNodes(state, data, mainNodes);
        state.log.trace(() -> "implied shapes: " + impliedShapes.stream().map(s -> s.getShapeNode().toString()).collect(Collectors.joining(", ")));
        return impliedShapes;
    }


    public static Set<BindingValidationResult> validateShapeOnData(BlendingInstance instance, Shape shape,
                    VariableBindings bindings, AlgorithmState state, VariableAwareGraph data) {
        state.log.trace(() -> "validating shape " + shape.getShapeNode() + " with bindings " + bindings
                            .getBindingsAsSet());
        Collection<Node> focusNodes = VLib.focusNodes(data, shape);
        data.reset();
        state.debugWriter.incIndent();
        Set<BindingValidationResult> results = new HashSet<>();
        state.log.trace(() -> String.format("found %d focus nodes", focusNodes.size()));
        for (Node node : focusNodes) {
            BindingValidationResult result = validateFocusNodeOnData(instance, state, shape, node, bindings, data);
            if (result != null) {
                results.add(result);
            }
        }
        state.debugWriter.decIndent();
        return results;
    }

    public static BindingValidationResult validateFocusNode(BlendingInstance instance, AlgorithmState state,
                    Shape shape,
                    Node focusNode, VariableBindings bindings) {
        VariableAwareGraph data = blendDataGraphs(instance, bindings);
        return validateFocusNodeOnData(instance, state, shape, focusNode, bindings, data);
    }

    public static BindingValidationResult validateFocusNodeOnData(BlendingInstance instance,
                    AlgorithmState state, Shape shape,
                    Node focusNode,
                    VariableBindings bindings,
                    VariableAwareGraph data) {
        ShaclErrorHandler errorHandler = new ShaclErrorHandler();
        ValidationContext shaclValidationContext = ValidationContext.create(state.shapes, data, errorHandler);
        state.debugWriter.incIndent();
        state.log.trace(() -> "Checking focus node " + focusNode);
        data.reset();
        VLib.validateShape(shaclValidationContext, data, shape, focusNode);
        Set<Node> encounteredVariables = data.getEncounteredVariables();
        BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
        encounteredVariables.addAll(instanceLogic.getVariablesUsedInShape(shape.getShapeNode()));
        findNamedShapesForErrorsInReport(shaclValidationContext.generateReport(), state.shapes)
                        .stream()
                        .flatMap(s -> instanceLogic.getVariablesUsedInShape(s).stream())
                        .forEach(encounteredVariables::add);
        encounteredVariables.removeAll(bindings.getDecidedVariables());
        if (encounteredVariables.isEmpty()) {
            if (shaclValidationContext.hasViolation()){
                state.log.trace(() -> "focus node is invalid - binding cannot lead to acceptable result");
                state.debugWriter.decIndent();
                state.forbiddenBindings.forbidBindings(bindings.getBindingsAsSet());
                return new BindingValidationResult(shape, focusNode, bindings, encounteredVariables, Ternary.FALSE,
                                Ternary.FALSE);
            } else {
                    state.log.trace(() -> "focus node is valid, no new variables encountered");
                state.debugWriter.decIndent();
                return new BindingValidationResult(shape, focusNode, bindings, encounteredVariables, Ternary.TRUE,
                                Ternary.UNKNOWN);
            }
        }
        state.log.trace(() ->
                        "encountered new variables: " + encounteredVariables.stream().map(Object::toString)
                                            .collect(joining(", ")));
        state.debugWriter.decIndent();
        return new BindingValidationResult(shape, focusNode, bindings, encounteredVariables, Ternary.UNKNOWN,
                        Ternary.UNKNOWN);
    }

    public static SearchNode validateSearchNode(BlendingInstance instance, AlgorithmState state, SearchNode searchNode) {
        state.log.trace(() -> "validating all shapes of search node");
        state.debugWriter.incIndent();
        Set<BindingValidationResult> validationResults = new HashSet<>();
        VariableBindings bindings = searchNode.bindings.getVariableBindings();
        Set<Shape> shapes = new HashSet<>(searchNode.shapes);
        VariableAwareGraph data = blendDataGraphs(instance, bindings);
        shapes.addAll(getShapesTargetedOnBindings(state, data, bindings));
        for (Shape toValidate: shapes) {
             Set<BindingValidationResult> results = ShaclValidator.validateShapeOnData(instance, toValidate, searchNode.bindings.getVariableBindings(), state, data);
             if (results.stream().anyMatch(vr -> vr.valid.isFalse() && vr.encounteredVariables.isEmpty())){
                 state.debugWriter.decIndent();
                 state.log.trace(() -> "all shapes of search node valid: " + Ternary.FALSE);
                 return new SearchNode(searchNode.shapes, searchNode.focusNodes, searchNode.bindings, searchNode.encounteredVariables, Ternary.FALSE, Ternary.FALSE);
             }
             validationResults.addAll(results);
        }
        Set<Node> encounteredVars = validationResults.stream().flatMap(r -> r.encounteredVariables.stream()).collect(Collectors.toSet());
        Set<Ternary> validities = validationResults.stream().map(r -> r.valid).collect(Collectors.toSet());
        Ternary valid = validities.stream().reduce((l,r) -> l.and(r)).get();
        CompactVariables encountered = state.bindingsManager.getCompactVariables(encounteredVars);
        state.debugWriter.decIndent();
        state.log.trace(() -> "all shapes of search node valid: " + valid);
        return new SearchNode(searchNode.shapes, searchNode.focusNodes, searchNode.bindings, encountered, valid, Ternary.UNKNOWN);
    }

    public static Set<BindingValidationResult> validateSearchNodeWithImpliedShapes(BlendingInstance instance,
                    AlgorithmState state, SearchNode searchNode, Set<Shape> omitShapes) {
        // now also check the shapes that are implied by the bound variables 
        // but collect newly encountered variables only if validation fails and there are any
        VariableBindings bindings = searchNode.bindings.getVariableBindings();
        Set<BindingValidationResult> resultsForImpliedShapes = validateBindingsWithImpliedShapes(
                        instance, state, bindings, omitShapes);
        if (resultsForImpliedShapes == null)
            return null;
        return resultsForImpliedShapes;
    }

    public static Set<BindingValidationResult> validateBindingsWithImpliedShapes(BlendingInstance instance,
                    AlgorithmState state, VariableBindings bindings, Set<Shape> omitShapes) {
        Set<BindingValidationResult> resultsForImpliedShapes = new HashSet<>();
        Set<Shape> impliedShapes = bindings.getDecidedVariables().stream().flatMap(v -> state.shapesImpliedByVariables.getOrDefault(v,
                        Collections.emptySet()).stream()).collect(Collectors.toSet());
        impliedShapes.removeAll(omitShapes);
        state.log.trace(() -> "checking implied shapes: " + impliedShapes);
        state.debugWriter.incIndent();
        for (Shape toValidate: impliedShapes){
            Set<BindingValidationResult> results = ShaclValidator.validateShape(instance, toValidate, bindings,
                            state);
            if (results.stream().anyMatch(vr -> vr.valid.isFalse())){
                Set<Node> newEncounteredVars = results.stream().flatMap(r -> r.encounteredVariables.stream()).collect(Collectors.toSet());
                if (newEncounteredVars.size() > 0) {
                    // encountered new vars testing implied shape, which fails - so we need to check if adding the
                    // variable makes the node valid
                    state.log.trace(() -> "invalid implied shape: " + toValidate);
                    state.log.trace(() -> "adding variables: " + newEncounteredVars);
                    resultsForImpliedShapes.addAll(results);
                } else {
                    state.debugWriter.decIndent();
                    state.log.trace(() -> "invalid implied shape, rejecting bindings");
                    return null;
                }
            }
        }
        state.debugWriter.decIndent();
        return resultsForImpliedShapes;
    }

    public static SearchNode validateSearchNodeGlobally(BlendingInstance instance, AlgorithmState state, SearchNode searchNode) {
        BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
        if (searchNode.valid.isFalse() || searchNode.encounteredVariables.size() > 0) {
            return searchNode;
        }
        VariableAwareGraph data = new VariableAwareGraph(instance.blendingBackground.combineWithBackgroundData(new BlendedGraphs(instance.leftTemplate.getTemplateGraphs()
                        .getDataGraph(), instance.rightTemplate.getTemplateGraphs().getDataGraph(),
                        searchNode.bindings.getBindingsAsSet())),
                        instanceLogic::isVariable);
        ShaclErrorHandler errorHandler = new ShaclErrorHandler();
        ValidationContext shaclValidationContext = ValidationContext.create(state.shapes, data, errorHandler);
        state.debugWriter.incIndent();
        state.log.trace(() -> "Checking if search node is globally valid ");
        // no more bindings needed to make final decision: evaluate with all shapes:
        boolean conforms = org.apache.jena.shacl.ShaclValidator.get().conforms(state.shapes, data);
        if (conforms) {
            state.log.trace(() -> "\tall shapes valid: " + Ternary.TRUE);
            state.debugWriter.decIndent();
            return searchNode.globallyValid(true);
        } else {
            state.log.trace(() -> "\tall shapes valid: " + Ternary.FALSE);
            state.debugWriter.decIndent();
            return searchNode.globallyValid(false);
        }
    }


    private static Set<Node> findNamedShapesForErrorsInReport(ValidationReport validationReport, Shapes shapes) {
        Set<Node> namedShapes = new HashSet<>();
        for (ReportEntry entry : validationReport.getEntries()) {
            namedShapes.addAll(findNamedShapeForShape(entry.source(), shapes));
        }
        return namedShapes;
    }

    private static Set<Node> findNamedShapeForShape(Node source, Shapes shapes) {
        return findNamedShapeForShape(source,shapes, 0);
    }

    private static Set<Node> findNamedShapeForShape(Node source, Shapes shapes, int depth) {
        if (depth > 50) {
            throw new IllegalStateException("Max recursion depth exceeded for this operation. Either the shapes are very deep or there is a cycle somewhere");
        }
        Shape sourceShape = shapes.getShape(source);
        if (sourceShape != null){
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
                        .flatMap(in -> findNamedShapeForShape(in, shapes, depth+1).stream())
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
            for( Node node: targetNodes) {
                if (VLib.isFocusNode(targetShape, node, data )) {
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
