package won.utils.blend.algorithm.sat.shacl2;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.astarish.AlgorithmState;
import won.utils.blend.algorithm.sat.shacl2.forbiddenbindings.VariableBindingSetForbiddenBindings;
import won.utils.blend.algorithm.sat.support.BindingValidity;
import won.utils.blend.algorithm.support.instance.BlendingInstanceLogic;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static won.utils.blend.algorithm.sat.shacl.ProcessingStepMessages.initialShapeMsg;

public abstract class BindingSetExtractor {
    public static Set<BindingValidity> extract(BlendingInstance instance) {
        IndentedWriter out = IndentedWriter.clone(IndentedWriter.stdout);
        Shapes shapes = getShapes(instance);
        ForbiddenBindings forbiddenBindings = new VariableBindingSetForbiddenBindings();
        ExtractionContext2 context = new ExtractionContext2(shapes, forbiddenBindings, out);
        Stream<BindingValidity> validities = collectBindingsPerTargetShape(instance, out, shapes, context);
        return validities.collect(toSet());
    }

    public static Stream<BindingValidity> collectBindingsPerTargetShape(BlendingInstance instance, IndentedWriter out,
                    Shapes shapes,
                    ExtractionContext2 context) {
        Stream<BindingValidity> result = shapes.getTargetShapes()
                        .stream()
                        .flatMap(s -> collectBindingsForShape(s, instance, context));
        return result;
    }

    public static Stream<BindingValidity> collectBindingsForShape(Shape shape, BlendingInstance instance,
                    ExtractionContext2 context) {
        context.indentedWriter.println(initialShapeMsg(shape));
        context.indentedWriter.incIndent();
        BlendingInstanceLogic instanceLogic = new BlendingInstanceLogic(instance);
        VariableBindings bindings = new VariableBindings(instanceLogic.getVariables());
        Stream<BindingValidity> bindingValidities = validateWithBindingsForAllEncounteredVariables(shape, bindings,
                        instance,
                        null);
        context.indentedWriter.decIndent();
        return bindingValidities;
    }

    private static Stream<BindingValidity> validateWithBindingsForAllEncounteredVariables(Shape shape,
                    VariableBindings bindings, BlendingInstance instance,
                    AlgorithmState context) {
        Set<BindingValidationResult> bvResult = ShaclValidator.validateShape(instance, shape, bindings, context);
        /*
         * if
         * (bindings.getDecidedVariables().containsAll(bvResult.encounteredVariables)) {
         * return Stream.of(bvResult.toBindingValidity()); } BindingCombinator
         * bindingCombinator = new BindingCombinator(instance, context); return
         * bindingCombinator.allCombinations(bvResult.encounteredVariables, bindings, vb
         * -> validateWithBindingsForAllEncounteredVariables(shape, vb, instance,
         * context)) .flatMap(Function.identity());
         */
        return null;
    }

    public static Shapes getShapes(BlendingInstance instance) {
        return new BlendingInstanceLogic(instance).getShapes();
    }
}
