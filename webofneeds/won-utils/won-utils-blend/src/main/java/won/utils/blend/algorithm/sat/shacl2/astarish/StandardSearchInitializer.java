package won.utils.blend.algorithm.sat.shacl2.astarish;

import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.BindingValidationResult;
import won.utils.blend.algorithm.sat.shacl2.ShaclValidator;
import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.Set;
import java.util.stream.Collectors;

import static won.utils.blend.algorithm.sat.shacl.ProcessingStepMessages.initialShapeMsg;

public class StandardSearchInitializer
                implements SearchInitializer {
    BlendingInstanceLogic instanceLogic = null;

    @Override
    public Set<SearchNode> initializeSearch(BlendingInstance instance, AlgorithmState state) {
        this.instanceLogic = new BlendingInstanceLogic(instance);
        Shapes shapes = instanceLogic.getShapes();
        return generateSearchNodesPerFocusNodes(instance, state);
    }

    public static Set<SearchNode> generateSearchNodesPerFocusNodes(BlendingInstance instance, AlgorithmState state) {
        SearchNodeFormatter formatter = new VerbosityAwareSearchNodeFormatter(state);
        Set<BindingValidationResult> validationResults = state.shapes.getTargetShapes()
                        .stream()
                        .flatMap(s -> collectBindingsForShape(s, instance, state).stream())
                        .collect(Collectors.toSet());
        return validationResults
                        .stream()
                        .filter(vr -> vr.valid.isTrueOrUnknown())
                        .map(vr -> {
                            SearchNode searchNode = new SearchNode(
                                            Set.of(vr.shape),
                                            Set.of(vr.node),
                                            state.bindingsManager.fromBindings(vr.bindings.getBindingsAsSet()),
                                            state.bindingsManager.getCompactVariables(vr.encounteredVariables),
                                            vr.valid,
                                            Ternary.UNKNOWN);
                            state.log.info(() -> "Generated initial search node");
                            state.debugWriter.incIndent();
                            state.log.info(() -> formatter.format(searchNode, state.bindingsManager));
                            state.debugWriter.decIndent();
                            return searchNode;
                        }).collect(Collectors.toSet());
    }

    public static Set<BindingValidationResult> collectBindingsForShape(Shape shape, BlendingInstance instance,
                    AlgorithmState state) {
        state.log.info(() -> initialShapeMsg(shape));
        state.debugWriter.incIndent();
        BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
        VariableBindings bindings = new VariableBindings(instanceLogic.getVariables());
        Set<BindingValidationResult> result = ShaclValidator.validateShape(instance, shape, bindings, state);
        state.debugWriter.decIndent();
        return result;
    }
}
