package won.utils.blend.support.stats;

import org.apache.jena.graph.Node;
import won.utils.blend.Template;
import won.utils.blend.support.bindings.VariableBinding;
import won.utils.blend.support.bindings.VariableBindings;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class BindingResultStatsAccumulator {
    public static BlendingResultStats accumulate(Template inputLeft, Template inputRight, Set<Template> result) {
        Set<GroupedTemplateStats> groupedTemplateStats = aggregateGroupedTemplateStats(result);
        Map<Node, Set<Node>> boundValuesByVariable = getBoundValuesByVariable(result);
        Set<VariableBinding> fixedBindings = Stream
                        .concat(
                                        inputLeft.getFixedBindings().getBindingsAsSet().stream(),
                                        inputRight.getFixedBindings().getBindingsAsSet().stream())
                        .collect(Collectors.toSet());
        Set<Node> variables = Stream.concat(
                        inputLeft.getVariables().stream(),
                        inputRight.getVariables().stream()).collect(
                                        Collectors.toSet());
        Set<Node> constants = Stream.concat(
                        inputLeft.getConstants().stream(),
                        inputRight.getConstants().stream())
                        .collect(Collectors.toSet());
        Set<VariableBindings> allBindings = result.stream().map(t -> t.getFixedBindings())
                        .collect(Collectors.toUnmodifiableSet());
        return new BlendingResultStats(boundValuesByVariable, result.size(), variables, constants, fixedBindings,
                        groupedTemplateStats, allBindings);
    }

    private static Map<Node, Set<Node>> getBoundValuesByVariable(
                    Set<Template> result) {
        return result.stream().flatMap(t -> t.getFixedBindings().getBindingsAsSet().stream())
                        .collect(Collectors.toMap(v -> v.getVariable(), v -> Set.of(v.getBoundNode()), (l, r) -> {
                            Set<Node> both = new HashSet<>(l);
                            both.addAll(r);
                            return both;
                        }));
    }

    private static Set<GroupedTemplateStats> aggregateGroupedTemplateStats(
                    Set<Template> result) {
        return result
                        .stream()
                        .map(blendingResult -> new TemplateStats(blendingResult.getTemplateGraphs().getTemplateNode(),
                                        new BindingStats(blendingResult.getFixedBindings().getUnboundVariables().size(),
                                                        blendingResult.getFixedBindings().getVariablesBoundToConstants().size(),
                                                        blendingResult.getFixedBindings().getVariablesBoundToVariables().size())))
                        .collect(Collectors
                                        .groupingBy(s -> s.bindingStats,
                                                        Collectors.mapping(TemplateStats::getTemplateNode,
                                                                        Collectors.toSet())))
                        .entrySet()
                        .stream()
                        .map(e -> new GroupedTemplateStats(e.getValue(), e.getKey())).collect(
                                        Collectors.toSet());
    }
}
