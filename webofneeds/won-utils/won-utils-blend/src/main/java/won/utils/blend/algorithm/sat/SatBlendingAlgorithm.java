package won.utils.blend.algorithm.sat;

import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl.BindingExtractionState;
import won.utils.blend.algorithm.sat.shacl.BindingSetExtractor;
import won.utils.blend.algorithm.sat.shacl.CheckedBindings;
import won.utils.blend.support.graph.TemplateGraphs;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class SatBlendingAlgorithm implements BlendingAlgorithm {
    @Override
    public Stream<TemplateGraphs> blend(BlendingInstance blendingInstanceContext) {
        Set<BindingExtractionState> states = BindingSetExtractor
                        .extract(blendingInstanceContext);
        Set<CheckedBindings> checkedBindings = states.stream().flatMap(s -> s.checkedBindings.stream()).collect(
                        Collectors.toSet());
        checkedBindings = BindingExtractionState.consolidate(checkedBindings);
        // System.out.println(resolveCheckedBindings(bindings).stream().sorted(new
        // CheckedBindingsComparator()).map(Object::toString).collect(joining("\n")));
        System.out.println("checked bindings: " + checkedBindings.size());
        System.out.println(checkedBindings.stream().sorted().map(Object::toString).collect(joining("\n")));
        // return bindings.stream().map(c -> c.bindings).collect(Collectors.toSet());
        return Stream.empty();
    }
}
