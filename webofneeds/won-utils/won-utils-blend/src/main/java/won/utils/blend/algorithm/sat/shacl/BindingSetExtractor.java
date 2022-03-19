package won.utils.blend.algorithm.sat.shacl;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.support.Ternary;
import won.utils.blend.support.bindings.VariableBinding;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static won.utils.blend.algorithm.sat.shacl.ProcessingStepMessages.initialFocusNodeMsg;
import static won.utils.blend.algorithm.sat.shacl.ProcessingStepMessages.initialShapeMsg;
import static won.utils.blend.algorithm.support.BlendingUtils.combineGraphsIfPresent;

public class BindingSetExtractor {
    /**
     * Extract the binding sets
     *
     * @return
     */
    public static Set<BindingExtractionState> extract(BlendingInstance instance) {
        IndentedWriter out = IndentedWriter.clone(IndentedWriter.stdout);
        Shapes shapes = Shapes.parse(
                        instance.blendingBackground.combineWithBackgroundShapes(
                                        combineGraphsIfPresent(
                                                        instance.leftTemplate.getTemplateGraphs().getShapesGraph(),
                                                        instance.rightTemplate.getTemplateGraphs().getShapesGraph())));
        Set<VariableBinding> forbiddenBindings = new HashSet<>();
        ExtractionContext context = new ExtractionContext(shapes, forbiddenBindings, out);
        Set<BindingExtractionState> result = shapes.getTargetShapes()
                        .stream()
                        .flatMap(shape -> {
                            out.println(initialShapeMsg(shape));
                            out.incIndent();
                            Stream<BindingExtractionState> ret = extractBindings(new BindingExtractionState(instance),
                                            shape, context).stream();
                            out.decIndent();
                            return ret;
                        })
                        .collect(Collectors.toSet());
        int size = 0;
        Set<CheckedBindings> allCheckedBindings = result
                        .stream()
                        .flatMap(s -> s.checkedBindings.stream())
                        .collect(Collectors.toSet());
        allCheckedBindings.addAll(
                        context.forbiddenBindings
                                        .stream()
                                        .map(b -> new CheckedBindings(List.of(b), Ternary.FALSE))
                                        .collect(Collectors.toSet()));
        Set<CheckedBindings> invalidBindingsOfSize = Collections.emptySet();
        Set<CheckedBindings> filteredCheckedBindings = new HashSet<>(allCheckedBindings);
        while (size == 0 || !invalidBindingsOfSize.isEmpty()) {
            size++;
            int finalSize = size;
            invalidBindingsOfSize = allCheckedBindings.stream()
                            .filter(cb -> cb.bindings.size() == finalSize)
                            .filter(cb -> cb.valid.isFalse())
                            .collect(Collectors.toSet());
            Set<CheckedBindings> finalInvalidBindingsOfSize = invalidBindingsOfSize;
            filteredCheckedBindings = filteredCheckedBindings
                            .stream()
                            .filter(b -> b.bindings.size() > finalSize &&
                                            finalInvalidBindingsOfSize
                                                            .stream()
                                                            .noneMatch(cb -> cb.bindings
                                                                            .stream()
                                                                            .allMatch(vb -> b.bindings.contains(vb))))
                            .collect(Collectors.toSet());
        }
        return Set.of(new BindingExtractionState(instance).addCheckedBindings(filteredCheckedBindings));
    }

    public static Set<BindingExtractionState> extractBindings(BindingExtractionState state, Shape shape,
                    ExtractionContext context) {
        Collection<Node> focusNodes = BindingSetExtractingVLib.focusNodes(state.getBlendedData(), shape);
        Set<BindingExtractionState> validationStates = new HashSet<>();
        for (Node focusNode : focusNodes) {
            context.indentedWriter.println(initialFocusNodeMsg(focusNode));
            context.indentedWriter.incIndent();
            BindingExtractionState resultState = processShape(state, shape, focusNode, context);
            context.indentedWriter.decIndent();
            validationStates.add(resultState);
        }
        return validationStates;
    }

    private static BindingExtractionState processShape(BindingExtractionState state, Shape shape, Node focusNode,
                    ExtractionContext context) {
        if (shape instanceof NodeShape) {
            return BindingSetExtractingVLib
                            .processNodeShape(state.setNodeAndResetValueNodes(focusNode), (NodeShape) shape, context);
        } else {
            return BindingSetExtractingVLib
                            .processPropertyShape(state.setNodeAndResetValueNodes(focusNode), (PropertyShape) shape,
                                            context);
        }
    }
}
