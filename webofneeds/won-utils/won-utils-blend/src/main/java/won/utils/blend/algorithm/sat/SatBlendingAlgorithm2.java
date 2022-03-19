package won.utils.blend.algorithm.sat;

import won.utils.blend.algorithm.BlendingAlgorithm;
import won.utils.blend.algorithm.BlendingInstance;
import won.utils.blend.algorithm.sat.shacl2.BindingSetExtractor;
import won.utils.blend.algorithm.sat.support.BindingValidity;
import won.utils.blend.support.graph.TemplateGraphs;

import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class SatBlendingAlgorithm2 implements BlendingAlgorithm {
    @Override
    public Stream<TemplateGraphs> blend(BlendingInstance blendingInstance) {
        Set<BindingValidity> validities = BindingSetExtractor
                        .extract(blendingInstance);
        System.out.println("valid bindings: " + validities.size());
        System.out.println(validities.stream().sorted().map(Object::toString).collect(joining("\n")));
        // return bindings.stream().map(c -> c.bindings).collect(Collectors.toSet());
        return Stream.empty();
    }
}
