package won.utils.blend.algorithm.sat.shacl2.astarish.expand;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.BindingCombinator;
import won.utils.blend.algorithm.sat.shacl2.BindingValidationResult;
import won.utils.blend.algorithm.sat.shacl2.ShaclValidator;
import won.utils.blend.algorithm.sat.shacl2.astarish.AlgorithmState;
import won.utils.blend.algorithm.sat.shacl2.astarish.ExpansionStrategy;
import won.utils.blend.algorithm.sat.shacl2.astarish.SearchNode;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;
import won.utils.blend.support.graph.VariableAwareGraphImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class ExploreAllOptionsStrategy implements ExpansionStrategy {
    @Override
    public Set<SearchNode> findSuccessors(BlendingInstance instance, AlgorithmState state,
                    SearchNode node) {
        if (node.encounteredVariables.size() == 0 && node.valid.isKnown()) {
            state.log.info(() -> "no additional variables encountered and validity is known - not expanding search node");
            return Collections.emptySet();
        }
        return generateSuccessorNodesForAllBindingCombinations(instance, state, node);
    }

    private static void removeOptionsOfSingleVariableFailingValidation(AlgorithmState state,
                    Set<BindingValidationResult> validationResults) {
        Set<VariableBinding> optionsToRemove = validationResults
                        .stream()
                        .filter(vr -> vr.valid.isFalse() && vr.encounteredVariables.isEmpty()
                                        && vr.bindings.sizeExcludingExplicitlyUnbound() == 1)
                        .flatMap(vr -> vr.bindings.getBindingsAsSet().stream())
                        .collect(Collectors.toSet());
        if (!optionsToRemove.isEmpty()) {
            state.log.trace(() -> "Removing " + optionsToRemove.size()
                            + " options that cannot possibly lead to an acceptable result");
            state.bindingsManager = state.bindingsManager.removeOptions(optionsToRemove);
        }
    }

    public static Set<SearchNode> generateSuccessorNodesForAllBindingCombinations(BlendingInstance instance,
                    AlgorithmState state,
                    SearchNode node) {
        BindingCombinator bindingCombinator = new BindingCombinator(instance, state);
        Set<Node> variables = node.encounteredVariables.getVariables();
        BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
        VariableBindings bindings = node.bindings.getVariableBindings();
        variables.removeAll(bindings.getDecidedVariables());
        state.log.info(() -> "generating all bindings combinations for newly encountered variables: " + variables);
        return bindingCombinator
                        .allCombinations(variables, bindings,
                                        vb -> generateSearchNodeForBindings(node.shapes, vb, instance, state))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toSet());
    }

    public static Optional<SearchNode> generateSearchNodeForBindings(Set<Shape> shapes, VariableBindings bindings,
                    BlendingInstance instance, AlgorithmState state) {
        state.debugWriter.incIndent();
        VariableAwareGraphImpl data = ShaclValidator.blendDataGraphs(instance, bindings);
        Set<Shape> impliedShapes = ShaclValidator.getShapesTargetedOnBindings(state, data, bindings);
        Set<Shape> shapesToCheck = new HashSet<>(shapes);
        shapesToCheck.addAll(impliedShapes);
        state.log.trace(() -> "validating with shapes: " + shapesToCheck.stream().map(s -> s.getShapeNode().toString())
                        .collect(Collectors.joining(", ")));
        Set<BindingValidationResult> result = new HashSet<>();
        for (Shape shape : shapesToCheck) {
            Set<BindingValidationResult> resultsForShape = ShaclValidator.validateShapeOnData(instance, shape, bindings,
                            state, data);
            result.addAll(resultsForShape);
            if (resultsForShape.stream().anyMatch(r -> r.valid.isFalse() && r.encounteredVariables.isEmpty())) {
                removeOptionsOfSingleVariableFailingValidation(state, result);
                state.debugWriter.decIndent();
                return SearchNode.of(state.bindingsManager, result.toArray(new BindingValidationResult[result.size()]));
            }
        }
        removeOptionsOfSingleVariableFailingValidation(state, result);
        state.debugWriter.decIndent();
        return SearchNode.of(state.bindingsManager, result.toArray(new BindingValidationResult[result.size()]));
    }
}
